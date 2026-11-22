package com.couplecalendar.server.event;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 일정이 추가·수정·삭제됐다는 알림 (DB 커밋 뒤 카톡 알림에 쓴다).
 * actor 는 그 일을 한 사람(A/B). 기기에서 "나는 누구"를 안 골랐으면 null.
 */
public record EventChanged(
		Long calendarId,
		String calendarCode,
		Action action,
		Owner actor,
		String actorName,
		String title,
		LocalDate startDate,
		LocalDate endDate,
		LocalTime startTime,
		LocalTime endTime,
		String location,
		Owner owner) {

	public enum Action {
		CREATED("추가"), UPDATED("수정"), DELETED("삭제");

		public final String label;

		Action(String label) {
			this.label = label;
		}
	}
}
