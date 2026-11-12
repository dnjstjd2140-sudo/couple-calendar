package com.couplecalendar.server.calendar;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.couplecalendar.server.common.NotFoundException;

@Service
public class CalendarService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final CalendarRepository calendars;

	public CalendarService(CalendarRepository calendars) {
		this.calendars = calendars;
	}

	@Transactional
	public CoupleCalendar create() {
		return calendars.save(new CoupleCalendar(newCode(), "나", "너"));
	}

	@Transactional(readOnly = true)
	public CoupleCalendar get(String code) {
		return calendars.findByCode(code)
				.orElseThrow(() -> new NotFoundException("달력을 찾을 수 없어요."));
	}

	@Transactional
	public CoupleCalendar rename(String code, String memberAName, String memberBName) {
		CoupleCalendar calendar = get(code);
		calendar.rename(memberAName.strip(), memberBName.strip());
		return calendar;
	}

	/** 추측할 수 없는 22자 링크 코드 (128bit). */
	private static String newCode() {
		byte[] bytes = new byte[16];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
