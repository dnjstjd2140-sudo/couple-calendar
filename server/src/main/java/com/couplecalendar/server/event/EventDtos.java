package com.couplecalendar.server.event;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class EventDtos {
	private EventDtos() {
	}

	public record EventRequest(
			@NotBlank @Size(max = 100) String title,
			@NotNull LocalDate startDate,
			LocalDate endDate,
			LocalTime startTime,
			LocalTime endTime,
			@Size(max = 100) String location,
			@Size(max = 2000) String memo,
			@NotNull Owner owner) {
	}

	public record EventResponse(
			Long id, String title, LocalDate startDate, LocalDate endDate,
			LocalTime startTime, LocalTime endTime, String location, String memo, Owner owner) {
		public static EventResponse from(CalendarEvent e) {
			return new EventResponse(e.getId(), e.getTitle(), e.getStartDate(), e.getEndDate(),
					e.getStartTime(), e.getEndTime(), e.getLocation(), e.getMemo(), e.getOwner());
		}
	}
}
