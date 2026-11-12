package com.couplecalendar.server.calendar;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.couplecalendar.server.calendar.CalendarDtos.CalendarResponse;
import com.couplecalendar.server.calendar.CalendarDtos.RenameRequest;

@RestController
@RequestMapping("/api/calendars")
public class CalendarController {

	private final CalendarService service;

	public CalendarController(CalendarService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CalendarResponse create() {
		return CalendarResponse.from(service.create());
	}

	@GetMapping("/{code}")
	public CalendarResponse get(@PathVariable String code) {
		return CalendarResponse.from(service.get(code));
	}

	@PatchMapping("/{code}")
	public CalendarResponse rename(@PathVariable String code, @Validated @RequestBody RenameRequest req) {
		return CalendarResponse.from(service.rename(code, req.memberAName(), req.memberBName()));
	}
}
