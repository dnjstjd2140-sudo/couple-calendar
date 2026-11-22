package com.couplecalendar.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // 카톡 알림을 응답과 별도로 보냄
public class CoupleCalendarApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoupleCalendarApplication.class, args);
	}
}
