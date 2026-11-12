package com.couplecalendar.server.event;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "events", indexes = @Index(name = "idx_events_calendar_dates", columnList = "calendarId,startDate,endDate"))
public class CalendarEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long calendarId;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false)
	private LocalDate startDate;

	@Column(nullable = false)
	private LocalDate endDate;

	/** null 이면 하루 종일 */
	private LocalTime startTime;
	private LocalTime endTime;

	@Column(length = 100)
	private String location;

	@Column(length = 2000)
	private String memo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Owner owner;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected CalendarEvent() {
	}

	public CalendarEvent(Long calendarId) {
		this.calendarId = calendarId;
		this.createdAt = Instant.now();
	}

	public void apply(EventDtos.EventRequest req) {
		LocalDate end = req.endDate() == null ? req.startDate() : req.endDate();
		if (end.isBefore(req.startDate())) {
			throw new IllegalArgumentException("끝나는 날이 시작하는 날보다 빨라요.");
		}
		LocalTime start = req.startTime();
		LocalTime finish = start == null ? null : req.endTime();
		if (finish != null && end.equals(req.startDate()) && finish.isBefore(start)) {
			throw new IllegalArgumentException("끝나는 시간이 시작 시간보다 빨라요.");
		}
		this.title = req.title().strip();
		this.startDate = req.startDate();
		this.endDate = end;
		this.startTime = start;
		this.endTime = finish;
		this.location = blankToNull(req.location());
		this.memo = blankToNull(req.memo());
		this.owner = req.owner();
		this.updatedAt = Instant.now();
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.strip();
	}

	public Long getId() { return id; }
	public Long getCalendarId() { return calendarId; }
	public String getTitle() { return title; }
	public LocalDate getStartDate() { return startDate; }
	public LocalDate getEndDate() { return endDate; }
	public LocalTime getStartTime() { return startTime; }
	public LocalTime getEndTime() { return endTime; }
	public String getLocation() { return location; }
	public String getMemo() { return memo; }
	public Owner getOwner() { return owner; }
	public Instant getUpdatedAt() { return updatedAt; }
}
