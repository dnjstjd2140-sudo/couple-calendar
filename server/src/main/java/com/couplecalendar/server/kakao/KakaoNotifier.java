package com.couplecalendar.server.kakao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
 * 일정이 바뀌면 원성-민경 카톡 1:1 채팅방으로 알린다 (카카오 "친구에게 보내기").
 * 바꾼 사람의 카톡 계정으로 상대에게 보낸다. "같이" 일정도 같은 방이라 한 번만 보낸다.
 * 1:1 로 못 보내면 (친구 아님, 앱 팀원 아님 등) 상대의 "나와의 채팅"으로 대신 보낸다.
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
		Map<Owner, KakaoToken> byMember = new EnumMap<>(Owner.class);
		tokens.findByCalendarId(e.calendarId()).forEach(t -> byMember.put(t.getMember(), t));

		Owner sender = sender(e.actor(), byMember.keySet().contains(Owner.A));
		Owner receiver = other(sender);
		KakaoToken from = byMember.get(sender);
		KakaoToken to = byMember.get(receiver);
		String text = message(e);
		String url = kakao.baseUrl() + "/c/" + e.calendarCode();

		if (from != null && to != null && to.getKakaoUserId() != null && sendToFriend(from, to, text, url)) {
			return;
		}
		if (to != null) {
			sendToMe(to, text, url);
		}
	}

	/** 바꾼 사람이 보낸다. 누가 바꿨는지 모르면 연결된 사람 중에서 (원성 우선). */
	static Owner sender(Owner actor, boolean aConnected) {
		if (actor == Owner.A || actor == Owner.B) {
			return actor;
		}
		return aConnected ? Owner.A : Owner.B;
	}

	static Owner other(Owner o) {
		return o == Owner.A ? Owner.B : Owner.A;
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

	private boolean sendToFriend(KakaoToken from, KakaoToken to, String text, String url) {
		return withToken(from, accessToken -> {
			String uuid = kakao.friendUuid(accessToken, to.getKakaoUserId());
			if (uuid == null) {
				log.warn("1:1 채팅방으로 못 보냄: 카톡 친구 목록에 상대가 없음 (calendar={}, from={})",
						from.getCalendarId(), from.getMember());
				return false;
			}
			kakao.sendToFriend(accessToken, uuid, text, url);
			return true;
		});
	}

	private void sendToMe(KakaoToken to, String text, String url) {
		withToken(to, accessToken -> {
			kakao.sendToMe(accessToken, text, url);
			return true;
		});
	}

	private interface Call {
		boolean run(String accessToken);
	}

	/** 토큰을 (필요하면 갱신해서) 넣고 호출. 실패하면 로그만 남기고 false. */
	private boolean withToken(KakaoToken token, Call call) {
		try {
			if (token.accessExpiredSoon() && !refresh(token)) {
				return false;
			}
			try {
				return call.run(token.getAccessToken());
			} catch (HttpClientErrorException.Unauthorized expired) {
				return refresh(token) && call.run(token.getAccessToken());
			}
		} catch (RuntimeException ex) {
			log.warn("카톡 알림 실패 (calendar={}, member={}): {}",
					token.getCalendarId(), token.getMember(), ex.getMessage());
			return false;
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
