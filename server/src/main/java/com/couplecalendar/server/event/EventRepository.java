package com.couplecalendar.server.event;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<CalendarEvent, Long> {

	/** [from, to] 기간과 겹치는 일정 */
	@Query("""
			select e from CalendarEvent e
			where e.calendarId = :calendarId and e.startDate <= :to and e.endDate >= :from
			order by e.startDate, e.startTime nulls first, e.id
			""")
	List<CalendarEvent> findOverlapping(@Param("calendarId") Long calendarId,
			@Param("from") LocalDate from, @Param("to") LocalDate to);

	Optional<CalendarEvent> findByIdAndCalendarId(Long id, Long calendarId);
}
