package com.couplecalendar.server.kakao;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
			@JsonProperty("expires_in") Long expiresIn,
			// 토큰 갱신 응답에는 refresh_token(_expires_in) 이 없을 수 있다 → 기본형(long)이면 읽다가 실패하므로 Long
			@JsonProperty("refresh_token") String refreshToken,
			@JsonProperty("refresh_token_expires_in") Long refreshTokenExpiresIn,
			String scope) {

		/** "카카오톡 메시지 전송"과 "친구 목록"에 둘 다 동의했는지 (선택 동의라 빠질 수 있다) */
		public boolean canSendMessage() {
			if (scope == null) {
				return false;
			}
			List<String> scopes = java.util.Arrays.asList(scope.split(" "));
			return scopes.contains("talk_message") && scopes.contains("friends");
		}
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
		return AUTH + "/oauth/authorize?response_type=code&scope=talk_message,friends"
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

	/** 로그인한 사람의 카카오 회원번호 */
	public long myUserId(String accessToken) {
		Me me = http.get().uri(API + "/v2/user/me")
				.header("Authorization", "Bearer " + accessToken)
				.retrieve()
				.body(Me.class);
		return me.id();
	}

	record Me(long id) {
	}

	record Friend(Long id, String uuid, @JsonProperty("allowed_msg") Boolean allowedMsg) {
	}

	record Friends(List<Friend> elements, @JsonProperty("total_count") int totalCount) {
	}

	/** 내 카톡 친구 중에서 카카오 회원번호가 userId 인 사람의 uuid (메시지 받는 사람 지정용). 없으면 null. */
	public String friendUuid(String accessToken, long userId) {
		for (int offset = 0; offset < 1000; offset += 100) {
			Friends page = http.get()
					.uri(API + "/v1/api/talk/friends?limit=100&offset=" + offset)
					.header("Authorization", "Bearer " + accessToken)
					.retrieve()
					.body(Friends.class);
			if (page == null || page.elements() == null || page.elements().isEmpty()) {
				return null;
			}
			for (Friend f : page.elements()) {
				if (f.id() != null && f.id() == userId && !Boolean.FALSE.equals(f.allowedMsg())) {
					return f.uuid();
				}
			}
			if (offset + 100 >= page.totalCount()) {
				return null;
			}
		}
		return null;
	}

	/** 카톡 "나와의 채팅"으로 보낸다. */
	public void sendToMe(String accessToken, String text, String url) {
		post(accessToken, "/v2/api/talk/memo/default/send", template(text, url), null);
	}

	/** 보내는 사람(accessToken 주인)과 받는 사람(uuid)의 1:1 채팅방으로 보낸다. */
	public void sendToFriend(String accessToken, String receiverUuid, String text, String url) {
		post(accessToken, "/v1/api/talk/friends/message/default/send", template(text, url),
				json.writeValueAsString(List.of(receiverUuid)));
	}

	private Map<String, Object> template(String text, String url) {
		return Map.of(
				"object_type", "text",
				"text", text,
				"link", Map.of("web_url", url, "mobile_web_url", url),
				"button_title", "달력 열기");
	}

	private void post(String accessToken, String path, Map<String, Object> template, String receiverUuids) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("template_object", json.writeValueAsString(template));
		if (receiverUuids != null) {
			form.add("receiver_uuids", receiverUuids);
		}
		http.post().uri(API + path)
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
