package com.couplecalendar.server.calendar;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarRepository extends JpaRepository<CoupleCalendar, Long> {
	Optional<CoupleCalendar> findByCode(String code);
}
