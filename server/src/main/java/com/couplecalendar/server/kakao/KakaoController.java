package com.couplecalendar.server.kakao;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.couplecalendar.server.calendar.CalendarService;
import com.couplecalendar.server.calendar.CoupleCalendar;
import com.couplecalendar.server.event.Owner;

/**
 * 카톡 알림 연결.
 * 설정 화면 → GET /api/calendars/{code}/kakao/connect?member=A → 카카오 로그인·동의
 * → GET /api/kakao/callback → 토큰 저장 → 달력(/c/{code}?kakao=ok) 으로 돌아감.
 */
@RestController
public class KakaoController {

	private static final Logger log = LoggerFactory.getLogger(KakaoController.class);

	private final KakaoClient kakao;
	private final KakaoTokenRepository tokens;
	private final CalendarService calendars;

	public KakaoController(KakaoClient kakao, KakaoTokenRepository tokens, CalendarService calendars) {
		this.kakao = kakao;
		this.tokens = tokens;
		this.calendars = calendars;
	}

	public record KakaoStatus(boolean available, boolean a, boolean b) {
	}

	@GetMapping("/api/calendars/{code}/kakao")
	public KakaoStatus status(@PathVariable String code) {
		Long calendarId = calendars.get(code).getId();
		boolean a = false;
		boolean b = false;
		for (KakaoToken t : tokens.findByCalendarId(calendarId)) {
			if (t.getMember() == Owner.A) a = true;
			if (t.getMember() == Owner.B) b = true;
		}
		return new KakaoStatus(kakao.enabled(), a, b);
	}

	@GetMapping("/api/calendars/{code}/kakao/connect")
	public ResponseEntity<Void> connect(@PathVariable String code, @RequestParam Owner member) {
		if (!kakao.enabled()) {
			throw new IllegalArgumentException("서버에 카카오 키가 설정되지 않았어요.");
		}
		requirePerson(member);
		calendars.get(code); // 없는 달력이면 404
		return redirect(kakao.authorizeUrl(code + "." + member.name()));
	}

	@GetMapping("/api/kakao/callback")
	@Transactional
	public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			@RequestParam(required = false) String error) {
		int dot = state == null ? -1 : state.lastIndexOf('.');
		if (dot < 0) {
			return redirect(kakao.baseUrl() + "/");
		}
		String calendarCode = state.substring(0, dot);
		String back = kakao.baseUrl() + "/c/" + calendarCode;
		if (error != null || code == null) {
			return redirect(back + "?kakao=cancel");
		}
		try {
			Owner member = Owner.valueOf(state.substring(dot + 1));
			requirePerson(member);
			CoupleCalendar calendar = calendars.get(calendarCode);
			KakaoClient.TokenResponse t = kakao.exchangeCode(code);
			if (!t.canSendMessage()) {
				// 로그인은 했지만 "카카오톡 메시지 전송"을 체크하지 않음 → 저장해도 알림을 못 보내므로 연결하지 않는다
				return redirect(back + "?kakao=noscope");
			}
			KakaoToken token = tokens.findByCalendarIdAndMember(calendar.getId(), member)
					.orElseGet(() -> new KakaoToken(calendar.getId(), member));
			token.apply(t);
			tokens.save(token);
			return redirect(back + "?kakao=ok");
		} catch (RuntimeException e) {
			log.warn("카카오 연결 실패: {}", e.getMessage());
			return redirect(back + "?kakao=fail");
		}
	}

	@DeleteMapping("/api/calendars/{code}/kakao/{member}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void disconnect(@PathVariable String code, @PathVariable Owner member) {
		Long calendarId = calendars.get(code).getId();
		tokens.findByCalendarIdAndMember(calendarId, member).ifPresent(tokens::delete);
	}

	private static void requirePerson(Owner member) {
		if (member == Owner.BOTH) {
			throw new IllegalArgumentException("A 또는 B 만 연결할 수 있어요.");
		}
	}

	private static ResponseEntity<Void> redirect(String url) {
		return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
	}
}
