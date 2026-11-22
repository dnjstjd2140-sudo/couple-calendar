package com.couplecalendar.server.kakao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.couplecalendar.server.event.EventChanged;
import com.couplecalendar.server.event.Owner;

class KakaoNotifierTest {

	@Test
	void 바꾼_사람이_상대에게_보낸다() {
		assertEquals(Owner.A, KakaoNotifier.sender(Owner.A, true));
		assertEquals(Owner.B, KakaoNotifier.sender(Owner.B, true));
		assertEquals(Owner.B, KakaoNotifier.other(Owner.A));
		assertEquals(Owner.A, KakaoNotifier.other(Owner.B));
	}

	@Test
	void 누가_바꿨는지_모르면_연결된_사람이_보낸다() {
		assertEquals(Owner.A, KakaoNotifier.sender(null, true));
		assertEquals(Owner.B, KakaoNotifier.sender(null, false));
	}

	@Test
	void 메시지_형식() {
		EventChanged e = new EventChanged(1L, "code", EventChanged.Action.CREATED, Owner.B, "민경",
				"성수 데이트", LocalDate.of(2026, 11, 29), LocalDate.of(2026, 11, 29),
				LocalTime.of(14, 0), LocalTime.of(16, 0), "성수동", Owner.BOTH);
		assertEquals("민경님이 일정을 추가했어요\n\n📅 성수 데이트\n🕒 11/29(일) 14:00 – 16:00\n📍 성수동",
				KakaoNotifier.message(e));
	}
}
