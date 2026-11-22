package com.couplecalendar.server.kakao;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.couplecalendar.server.event.Owner;

/** 달력의 한 사람(A 또는 B)이 "카톡 알림 받기"로 연결한 카카오 토큰. 카톡 "나에게 보내기"에 쓴다. */
@Entity
@Table(name = "kakao_tokens", uniqueConstraints = @UniqueConstraint(columnNames = { "calendarId", "member" }))
public class KakaoToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long calendarId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Owner member;

	/** 카카오 회원번호. 상대가 1:1 채팅방으로 보낼 때 친구 목록에서 이 사람을 찾는 데 쓴다. */
	private Long kakaoUserId;

	@Column(nullable = false, length = 1000)
	private String accessToken;

	@Column(nullable = false)
	private Instant accessExpiresAt;

	@Column(nullable = false, length = 1000)
	private String refreshToken;

	@Column(nullable = false)
	private Instant refreshExpiresAt;

	protected KakaoToken() {
	}

	public KakaoToken(Long calendarId, Owner member) {
		this.calendarId = calendarId;
		this.member = member;
	}

	/** 토큰 발급·갱신 응답을 반영. 갱신 응답에는 refresh_token 이 없을 수 있다. */
	public void apply(KakaoClient.TokenResponse t) {
		Instant now = Instant.now();
		this.accessToken = t.accessToken();
		this.accessExpiresAt = now.plusSeconds(t.expiresIn());
		if (t.refreshToken() != null) {
			this.refreshToken = t.refreshToken();
			this.refreshExpiresAt = now.plusSeconds(t.refreshTokenExpiresIn());
		}
	}

	public boolean accessExpiredSoon() {
		return Instant.now().plusSeconds(60).isAfter(accessExpiresAt);
	}

	public void setKakaoUserId(Long kakaoUserId) { this.kakaoUserId = kakaoUserId; }

	public Long getKakaoUserId() { return kakaoUserId; }
	public Long getCalendarId() { return calendarId; }
	public Owner getMember() { return member; }
	public String getAccessToken() { return accessToken; }
	public String getRefreshToken() { return refreshToken; }
	public Instant getRefreshExpiresAt() { return refreshExpiresAt; }
}
