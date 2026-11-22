package com.couplecalendar.server.kakao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.HttpClientErrorException;

import com.couplecalendar.server.event.EventChanged;
import com.couplecalendar.server.event.Owner;

/**
 * 일정이 바뀌면 카톡 "나에게 보내기"로 알린다.
 * 받는 사람: 한 사람의 일정이면 바꾼 사람의 상대만, "같이" 일정이면 둘 다.
 * (바꾼 사람을 모르면 둘 다)
 */
@Component
public class KakaoNotifier {

	private static final Logger log = LoggerFactory.getLogger(KakaoNotifier.class);
	private static final int MAX_TEXT = 200; // 카카오 텍스트 템플릿 최대 길이

	private final KakaoClient kakao;
	private final KakaoTokenRepository tokens;

	public KakaoNotifier(KakaoClient kakao, KakaoTokenRepository tokens) {
		this.kakao = kakao;
		this.tokens = tokens;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void on(EventChanged e) {
		if (!kakao.enabled()) {
			return;
		}
		Set<Owner> to = recipients(e.owner(), e.actor());
		String text = message(e);
		String url = kakao.baseUrl() + "/c/" + e.calendarCode();
		for (KakaoToken token : tokens.findByCalendarId(e.calendarId())) {
			if (to.contains(token.getMember())) {
				send(token, text, url);
			}
		}
	}

	static Set<Owner> recipients(Owner owner, Owner actor) {
		if (owner == Owner.BOTH || actor == null || actor == Owner.BOTH) {
			return EnumSet.of(Owner.A, Owner.B);
		}
		return EnumSet.of(actor == Owner.A ? Owner.B : Owner.A);
	}

	static String message(EventChanged e) {
		List<String> lines = new ArrayList<>();
		lines.add(e.actorName() == null
				? "일정이 " + e.action().label + "됐어요"
				: e.actorName() + "님이 일정을 " + e.action().label + "했어요");
		lines.add("");
		lines.add("📅 " + e.title());
		lines.add("🕒 " + when(e.startDate(), e.endDate(), e.startTime(), e.endTime()));
		if (e.location() != null) {
			lines.add("📍 " + e.location());
		}
		String text = String.join("\n", lines);
		return text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT - 1) + "…" : text;
	}

	private static String when(LocalDate start, LocalDate end, LocalTime startTime, LocalTime endTime) {
		String time = startTime == null ? "하루 종일"
				: endTime == null ? hm(startTime) : hm(startTime) + " – " + hm(endTime);
		if (!end.equals(start)) {
			return day(start) + " – " + day(end) + (startTime == null ? "" : " · " + time);
		}
		return day(start) + " " + time;
	}

	private static String day(LocalDate d) {
		return d.getMonthValue() + "/" + d.getDayOfMonth()
				+ "(" + d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN) + ")";
	}

	private static String hm(LocalTime t) {
		return String.format("%02d:%02d", t.getHour(), t.getMinute());
	}

	private void send(KakaoToken token, String text, String url) {
		try {
			if (token.accessExpiredSoon() && !refresh(token)) {
				return;
			}
			try {
				kakao.sendToMe(token.getAccessToken(), text, url);
			} catch (HttpClientErrorException.Unauthorized expired) {
				if (refresh(token)) {
					kakao.sendToMe(token.getAccessToken(), text, url);
				}
			}
		} catch (HttpClientErrorException.Forbidden noScope) {
			// "카카오톡 메시지 전송" 동의 철회 등: 연결을 풀어서 설정 화면에 "연결 안 됨"으로 보이게 한다
			log.info("카톡 메시지 권한 없음, 연결 해제 (calendar={}, member={})", token.getCalendarId(), token.getMember());
			tokens.delete(token);
		} catch (RuntimeException ex) {
			log.warn("카톡 알림 실패 (calendar={}, member={}): {}",
					token.getCalendarId(), token.getMember(), ex.getMessage());
		}
	}

	/** 토큰을 새로 받는다. 연결이 끊긴 경우(로그아웃·동의 철회·만료) 토큰을 지우고 false. */
	private boolean refresh(KakaoToken token) {
		try {
			token.apply(kakao.refresh(token.getRefreshToken()));
			tokens.save(token);
			return true;
		} catch (HttpClientErrorException ex) {
			log.info("카카오 연결 해제됨 (calendar={}, member={}): {}",
					token.getCalendarId(), token.getMember(), ex.getStatusCode());
			tokens.delete(token);
			return false;
		}
	}
}
