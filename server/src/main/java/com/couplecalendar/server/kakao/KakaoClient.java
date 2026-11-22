package com.couplecalendar.server.kakao;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;

/**
 * 카카오 OAuth + 카카오톡 "나에게 보내기" API.
 * 키는 server/.env 또는 환경변수 KAKAO_REST_API_KEY (선택: KAKAO_CLIENT_SECRET).
 */
@Component
public class KakaoClient {

	private static final String AUTH = "https://kauth.kakao.com";
	private static final String API = "https://kapi.kakao.com";

	private final RestClient http = RestClient.create();
	private final ObjectMapper json;
	private final String restApiKey;
	private final String clientSecret;
	private final String baseUrl;

	public KakaoClient(ObjectMapper json,
			@Value("${kakao.rest-api-key:}") String restApiKey,
			@Value("${kakao.client-secret:}") String clientSecret,
			@Value("${app.base-url}") String baseUrl) {
		this.json = json;
		this.restApiKey = restApiKey;
		this.clientSecret = clientSecret;
		this.baseUrl = baseUrl.replaceAll("/+$", "");
	}

	public record TokenResponse(
			@JsonProperty("access_token") String accessToken,
			@JsonProperty("expires_in") long expiresIn,
			@JsonProperty("refresh_token") String refreshToken,
			@JsonProperty("refresh_token_expires_in") long refreshTokenExpiresIn) {
	}

	public boolean enabled() {
		return !restApiKey.isBlank();
	}

	public String baseUrl() {
		return baseUrl;
	}

	public String redirectUri() {
		return baseUrl + "/api/kakao/callback";
	}

	public String authorizeUrl(String state) {
		return AUTH + "/oauth/authorize?response_type=code&scope=talk_message"
				+ "&client_id=" + enc(restApiKey)
				+ "&redirect_uri=" + enc(redirectUri())
				+ "&state=" + enc(state);
	}

	public TokenResponse exchangeCode(String code) {
		MultiValueMap<String, String> form = baseForm("authorization_code");
		form.add("redirect_uri", redirectUri());
		form.add("code", code);
		return token(form);
	}

	public TokenResponse refresh(String refreshToken) {
		MultiValueMap<String, String> form = baseForm("refresh_token");
		form.add("refresh_token", refreshToken);
		return token(form);
	}

	/** 카톡 "나와의 채팅"으로 텍스트 메시지를 보낸다. */
	public void sendToMe(String accessToken, String text, String url) {
		Map<String, Object> link = Map.of("web_url", url, "mobile_web_url", url);
		Map<String, Object> template = Map.of(
				"object_type", "text",
				"text", text,
				"link", link,
				"button_title", "달력 열기");
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("template_object", json.writeValueAsString(template));
		http.post().uri(API + "/v2/api/talk/memo/default/send")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.toBodilessEntity();
	}

	private MultiValueMap<String, String> baseForm(String grantType) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", grantType);
		form.add("client_id", restApiKey);
		if (!clientSecret.isBlank()) {
			form.add("client_secret", clientSecret);
		}
		return form;
	}

	private TokenResponse token(MultiValueMap<String, String> form) {
		return http.post().uri(AUTH + "/oauth/token")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(TokenResponse.class);
	}

	private static String enc(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}
}
