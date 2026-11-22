package com.couplecalendar.server.kakao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.couplecalendar.server.event.Owner;

public interface KakaoTokenRepository extends JpaRepository<KakaoToken, Long> {

	Optional<KakaoToken> findByCalendarIdAndMember(Long calendarId, Owner member);

	List<KakaoToken> findByCalendarId(Long calendarId);
}
