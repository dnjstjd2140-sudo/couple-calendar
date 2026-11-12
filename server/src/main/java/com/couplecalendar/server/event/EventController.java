package com.couplecalendar.server.event;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.couplecalendar.server.event.EventDtos.EventRequest;
import com.couplecalendar.server.event.EventDtos.EventResponse;

@RestController
@RequestMapping("/api/calendars/{code}/events")
public class EventController {

	private final EventService service;

	public EventController(EventService service) {
		this.service = service;
	}

	@GetMapping
	public List<EventResponse> list(@PathVariable String code,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return service.list(code, from, to).stream().map(EventResponse::from).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EventResponse create(@PathVariable String code, @Validated @RequestBody EventRequest req) {
		return EventResponse.from(service.create(code, req));
	}

	@PutMapping("/{id}")
	public EventResponse update(@PathVariable String code, @PathVariable Long id,
			@Validated @RequestBody EventRequest req) {
		return EventResponse.from(service.update(code, id, req));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable String code, @PathVariable Long id) {
		service.delete(code, id);
	}
}
