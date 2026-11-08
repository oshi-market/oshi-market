package com.oshimarket.domain.member.service;

import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 무차별 대입을 막는 로그인 시도 제한. 이메일별로 첫 실패부터 15분 안에 5번 틀리면 그 15분이 끝날 때까지 막는다.
 * - 없는 이메일도 똑같이 센다. 다르게 처리하면 응답 차이로 가입 여부가 드러난다.
 * - 서버 메모리에 기록하므로 서버를 재시작하면 초기화되고, 서버가 여러 대면 대수만큼 더 시도할 수 있다 (지금은 EC2 한 대).
 * - 남의 이메일로 일부러 틀려 그 사람을 막을 수 있지만, 막히는 시간이 15분으로 끝나서 영구 잠금보다 피해가 작다.
 */
@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    /** 기록이 이만큼 쌓이면 기간이 지난 것부터 지운다 (없는 이메일을 계속 바꿔 보내 메모리를 채우는 것 대비). */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Attempts> attemptsByEmail = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    /** 막힌 이메일이면 비밀번호를 확인하기 전에 거절한다 (맞는 비밀번호여도 막힌 동안은 거절). */
    public void checkAllowed(String email) {
        Attempts attempts = attemptsByEmail.get(email);
        if (attempts != null && !attempts.isExpired(clock.instant()) && attempts.failures() >= MAX_FAILURES) {
            throw new BusinessException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
        }
    }

    public void recordFailure(String email) {
        Instant now = clock.instant();
        if (attemptsByEmail.size() >= CLEANUP_THRESHOLD) {
            attemptsByEmail.values().removeIf(attempts -> attempts.isExpired(now));
        }
        // compute는 같은 이메일의 동시 실패를 하나씩 순서대로 반영해서 횟수가 빠지지 않는다
        attemptsByEmail.compute(email, (key, attempts) -> attempts == null || attempts.isExpired(now)
                ? new Attempts(1, now)
                : new Attempts(attempts.failures() + 1, attempts.windowStart()));
    }

    public void recordSuccess(String email) {
        attemptsByEmail.remove(email);
    }

    private record Attempts(int failures, Instant windowStart) {

        boolean isExpired(Instant now) {
            return !now.isBefore(windowStart.plus(WINDOW));
        }
    }
}
