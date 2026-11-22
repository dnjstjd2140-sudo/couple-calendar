package com.couplecalendar.server.kakao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.couplecalendar.server.event.EventChanged;
import com.couplecalendar.server.event.Owner;

class KakaoNotifierTest {

	@Test
	void 한사람_일정은_바꾼_사람의_상대만_받는다() {
		assertEquals(EnumSet.of(Owner.B), KakaoNotifier.recipients(Owner.A, Owner.A));
		assertEquals(EnumSet.of(Owner.B), KakaoNotifier.recipients(Owner.B, Owner.A)); // 원성이 민경 일정을 바꿔도 민경만
		assertEquals(EnumSet.of(Owner.A), KakaoNotifier.recipients(Owner.B, Owner.B));
	}

	@Test
	void 같이_일정은_둘_다_받는다() {
		assertEquals(EnumSet.of(Owner.A, Owner.B), KakaoNotifier.recipients(Owner.BOTH, Owner.A));
		assertEquals(EnumSet.of(Owner.A, Owner.B), KakaoNotifier.recipients(Owner.BOTH, Owner.B));
	}

	@Test
	void 누가_바꿨는지_모르면_둘_다_받는다() {
		assertEquals(EnumSet.of(Owner.A, Owner.B), KakaoNotifier.recipients(Owner.A, null));
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
