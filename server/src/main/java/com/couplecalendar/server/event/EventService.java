package com.couplecalendar.server.event;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.couplecalendar.server.calendar.CalendarService;
import com.couplecalendar.server.calendar.CoupleCalendar;
import com.couplecalendar.server.common.NotFoundException;
import com.couplecalendar.server.event.EventDtos.EventRequest;

@Service
public class EventService {

	private final CalendarService calendarService;
	private final EventRepository events;
	private final ApplicationEventPublisher publisher;

	public EventService(CalendarService calendarService, EventRepository events, ApplicationEventPublisher publisher) {
		this.calendarService = calendarService;
		this.events = events;
		this.publisher = publisher;
	}

	@Transactional(readOnly = true)
	public List<CalendarEvent> list(String code, LocalDate from, LocalDate to) {
		if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 120) {
			throw new IllegalArgumentException("조회 기간이 올바르지 않아요.");
		}
		Long calendarId = calendarService.get(code).getId();
		return events.findOverlapping(calendarId, from, to);
	}

	/** actor: 이 일을 한 사람 (A/B, 모르면 null). 카톡 알림 받는 사람을 정하는 데 쓴다. */
	@Transactional
	public CalendarEvent create(String code, EventRequest req, Owner actor) {
		CoupleCalendar calendar = calendarService.get(code);
		CalendarEvent event = new CalendarEvent(calendar.getId());
		event.apply(req);
		events.save(event);
		publish(calendar, EventChanged.Action.CREATED, actor, event);
		return event;
	}

	@Transactional
	public CalendarEvent update(String code, Long id, EventRequest req, Owner actor) {
		CoupleCalendar calendar = calendarService.get(code);
		CalendarEvent event = find(calendar, id);
		event.apply(req);
		publish(calendar, EventChanged.Action.UPDATED, actor, event);
		return event;
	}

	@Transactional
	public void delete(String code, Long id, Owner actor) {
		CoupleCalendar calendar = calendarService.get(code);
		CalendarEvent event = find(calendar, id);
		events.delete(event);
		publish(calendar, EventChanged.Action.DELETED, actor, event);
	}

	private CalendarEvent find(CoupleCalendar calendar, Long id) {
		return events.findByIdAndCalendarId(id, calendar.getId())
				.orElseThrow(() -> new NotFoundException("일정을 찾을 수 없어요."));
	}

	private void publish(CoupleCalendar calendar, EventChanged.Action action, Owner actor, CalendarEvent e) {
		if (actor == Owner.BOTH) {
			actor = null;
		}
		String actorName = actor == null ? null
				: actor == Owner.A ? calendar.getMemberAName() : calendar.getMemberBName();
		publisher.publishEvent(new EventChanged(calendar.getId(), calendar.getCode(), action, actor, actorName,
				e.getTitle(), e.getStartDate(), e.getEndDate(), e.getStartTime(), e.getEndTime(),
				e.getLocation(), e.getOwner()));
	}
}
