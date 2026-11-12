package com.couplecalendar.server.event;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.couplecalendar.server.calendar.CalendarService;
import com.couplecalendar.server.common.NotFoundException;
import com.couplecalendar.server.event.EventDtos.EventRequest;

@Service
public class EventService {

	private final CalendarService calendarService;
	private final EventRepository events;

	public EventService(CalendarService calendarService, EventRepository events) {
		this.calendarService = calendarService;
		this.events = events;
	}

	@Transactional(readOnly = true)
	public List<CalendarEvent> list(String code, LocalDate from, LocalDate to) {
		if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 120) {
			throw new IllegalArgumentException("조회 기간이 올바르지 않아요.");
		}
		Long calendarId = calendarService.get(code).getId();
		return events.findOverlapping(calendarId, from, to);
	}

	@Transactional
	public CalendarEvent create(String code, EventRequest req) {
		CalendarEvent event = new CalendarEvent(calendarService.get(code).getId());
		event.apply(req);
		return events.save(event);
	}

	@Transactional
	public CalendarEvent update(String code, Long id, EventRequest req) {
		CalendarEvent event = find(code, id);
		event.apply(req);
		return event;
	}

	@Transactional
	public void delete(String code, Long id) {
		events.delete(find(code, id));
	}

	private CalendarEvent find(String code, Long id) {
		Long calendarId = calendarService.get(code).getId();
		return events.findByIdAndCalendarId(id, calendarId)
				.orElseThrow(() -> new NotFoundException("일정을 찾을 수 없어요."));
	}
}
