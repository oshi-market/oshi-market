package com.oshimarket;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OshiMarketApplication {

	public static void main(String[] args) {
		// 서버 OS 시간대(EC2 기본 UTC 등)와 무관하게 한국 시간으로 고정. DB 기본 시간대도 V4에서 Asia/Seoul로 맞춤
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		SpringApplication.run(OshiMarketApplication.class, args);
	}

}
