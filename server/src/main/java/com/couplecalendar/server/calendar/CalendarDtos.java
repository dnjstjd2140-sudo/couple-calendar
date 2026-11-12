package com.couplecalendar.server.calendar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CalendarDtos {
	private CalendarDtos() {
	}

	public record RenameRequest(
			@NotBlank @Size(max = 20) String memberAName,
			@NotBlank @Size(max = 20) String memberBName) {
	}

	public record CalendarResponse(String code, String memberAName, String memberBName) {
		public static CalendarResponse from(CoupleCalendar c) {
			return new CalendarResponse(c.getCode(), c.getMemberAName(), c.getMemberBName());
		}
	}
}
