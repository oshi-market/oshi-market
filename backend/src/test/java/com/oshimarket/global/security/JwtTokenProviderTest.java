package com.oshimarket.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider("test-only-secret-not-used-outside-of-tests-32bytes", 60_000L);
        provider.init();
    }

    @Test
    void 발급한_토큰에서_회원번호를_꺼낼_수_있다() {
        String token = provider.createAccessToken(7L);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getMemberId(token)).isEqualTo(7L);
    }

    @Test
    void 토큰_내용에는_이메일이_들어가지_않는다() {
        String token = provider.createAccessToken(7L);

        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

        assertThat(payload).doesNotContain("email");
    }
}
