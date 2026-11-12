package com.couplecalendar.server.calendar;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 공유 링크 하나 = 달력 하나. 링크의 code 를 아는 사람만 보고 수정할 수 있다. */
@Entity
@Table(name = "calendars")
public class CoupleCalendar {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 32)
	private String code;

	@Column(nullable = false, length = 20)
	private String memberAName;

	@Column(nullable = false, length = 20)
	private String memberBName;

	@Column(nullable = false)
	private Instant createdAt;

	protected CoupleCalendar() {
	}

	public CoupleCalendar(String code, String memberAName, String memberBName) {
		this.code = code;
		this.memberAName = memberAName;
		this.memberBName = memberBName;
		this.createdAt = Instant.now();
	}

	public void rename(String memberAName, String memberBName) {
		this.memberAName = memberAName;
		this.memberBName = memberBName;
	}

	public Long getId() { return id; }
	public String getCode() { return code; }
	public String getMemberAName() { return memberAName; }
	public String getMemberBName() { return memberBName; }
	public Instant getCreatedAt() { return createdAt; }
}
