package com.couplecalendar.server.kakao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class KakaoTokenResponseTest {

	private final JsonMapper json = JsonMapper.builder().build();

	@Test
	void 갱신_응답에_refresh_token이_없어도_읽힌다() {
		// 카카오 토큰 갱신 응답 (refresh_token 유효기간이 많이 남았으면 새 refresh_token 을 안 준다)
		KakaoClient.TokenResponse t = json.readValue(
				"{\"access_token\":\"new\",\"token_type\":\"bearer\",\"expires_in\":21599}",
				KakaoClient.TokenResponse.class);
		assertEquals("new", t.accessToken());
		assertEquals(21599L, t.expiresIn());
		assertNull(t.refreshToken());
		assertNull(t.refreshTokenExpiresIn());
	}

	@Test
	void 처음_발급_응답() {
		KakaoClient.TokenResponse t = json.readValue("""
				{"access_token":"a","token_type":"bearer","refresh_token":"r","expires_in":21599,
				 "scope":"talk_message friends","refresh_token_expires_in":5183999}""",
				KakaoClient.TokenResponse.class);
		assertEquals("r", t.refreshToken());
		assertEquals(5183999L, t.refreshTokenExpiresIn());
		assertEquals(true, t.canSendMessage());
	}
}
