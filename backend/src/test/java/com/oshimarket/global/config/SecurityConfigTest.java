package com.oshimarket.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oshimarket.domain.chat.service.ChatService;
import com.oshimarket.domain.curation.service.CurationService;
import com.oshimarket.domain.item.service.ItemImageService;
import com.oshimarket.domain.item.service.ItemService;
import com.oshimarket.domain.member.service.MemberService;
import com.oshimarket.domain.review.service.ReviewService;
import com.oshimarket.domain.transaction.service.TransactionHistoryService;
import com.oshimarket.domain.transaction.service.TransactionService;
import com.oshimarket.domain.wishlist.service.WishlistService;
import com.oshimarket.domain.work.service.WorkService;
import com.oshimarket.global.security.JwtAuthenticationEntryPoint;
import com.oshimarket.global.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * SecurityConfig의 주소별 규칙(누가 어디에 들어갈 수 있는지)을 고정하는 테스트.
 * 웹 계층만 띄우고 서비스는 가짜(mock)로 바꿔 DB 없이 돌지만, 보안 설정과 JWT 필터는 실제 코드를 쓴다.
 * 컨트롤러에 API를 추가하면 아래 목록에도 추가해서 공개/보호 여부를 명시적으로 정한다.
 */
@WebMvcTest(properties = "jwt.secret=" + SecurityConfigTest.SECRET)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtAuthenticationEntryPoint.class})
class SecurityConfigTest {

    static final String SECRET = "test-only-secret-not-used-outside-of-tests-32bytes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private ItemService itemService;
    @MockitoBean private ItemImageService itemImageService;
    @MockitoBean private MemberService memberService;
    @MockitoBean private ChatService chatService;
    @MockitoBean private SimpMessagingTemplate messagingTemplate;
    @MockitoBean private TransactionService transactionService;
    @MockitoBean private TransactionHistoryService transactionHistoryService;
    @MockitoBean private ReviewService reviewService;
    @MockitoBean private WishlistService wishlistService;
    @MockitoBean private CurationService curationService;
    @MockitoBean private WorkService workService;

    /** 로그인 없이 들어갈 수 있어야 하는 주소 (둘러보기·가입·로그인). */
    static Stream<Arguments> publicEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/auth/signup"),
                Arguments.of(HttpMethod.POST, "/api/auth/login"),
                Arguments.of(HttpMethod.GET, "/api/items"),
                Arguments.of(HttpMethod.GET, "/api/items/1"),
                Arguments.of(HttpMethod.GET, "/api/items/1/wishlist"),
                Arguments.of(HttpMethod.GET, "/api/curations"),
                Arguments.of(HttpMethod.GET, "/api/curations/1"),
                Arguments.of(HttpMethod.GET, "/api/works"),
                Arguments.of(HttpMethod.GET, "/api/members/1"),
                Arguments.of(HttpMethod.GET, "/actuator/health"),
                Arguments.of(HttpMethod.GET, "/ws-chat")
        );
    }

    /** 로그인해야 들어갈 수 있는 주소. 본인 것인지(소유자·참여자) 검사는 각 서비스 테스트에서 한다. */
    static Stream<Arguments> protectedEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/items"),
                Arguments.of(HttpMethod.PATCH, "/api/items/1"),
                Arguments.of(HttpMethod.DELETE, "/api/items/1"),
                Arguments.of(HttpMethod.POST, "/api/items/1/images"),
                Arguments.of(HttpMethod.DELETE, "/api/items/1/images/1"),
                Arguments.of(HttpMethod.POST, "/api/items/1/wishlist"),
                Arguments.of(HttpMethod.DELETE, "/api/items/1/wishlist"),
                Arguments.of(HttpMethod.POST, "/api/items/1/chatrooms"),
                Arguments.of(HttpMethod.GET, "/api/members/me"),
                Arguments.of(HttpMethod.PATCH, "/api/members/me"),
                Arguments.of(HttpMethod.GET, "/api/members/me/chatrooms"),
                Arguments.of(HttpMethod.GET, "/api/members/me/transactions"),
                Arguments.of(HttpMethod.GET, "/api/members/me/wishlist"),
                Arguments.of(HttpMethod.GET, "/api/chatrooms/1/messages"),
                Arguments.of(HttpMethod.POST, "/api/chatrooms/1/transactions"),
                Arguments.of(HttpMethod.GET, "/api/transactions/1"),
                Arguments.of(HttpMethod.PATCH, "/api/transactions/1/status"),
                Arguments.of(HttpMethod.POST, "/api/transactions/1/reviews")
        );
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("publicEndpoints")
    void 공개_주소는_토큰_없이_들어갈_수_있다(HttpMethod method, String uri) throws Exception {
        mockMvc.perform(request(method, uri))
                .andExpect(notBlockedBySecurity());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("protectedEndpoints")
    void 보호된_주소는_토큰이_없으면_401(HttpMethod method, String uri) throws Exception {
        mockMvc.perform(request(method, uri))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("protectedEndpoints")
    void 보호된_주소는_올바른_토큰이면_들어갈_수_있다(HttpMethod method, String uri) throws Exception {
        mockMvc.perform(request(method, uri).header("Authorization", bearer(jwtTokenProvider.createAccessToken(1L))))
                .andExpect(notBlockedBySecurity());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/actuator/prometheus", "/actuator/info", "/actuator/env"})
    void 운영용_actuator는_토큰이_없으면_401(String uri) throws Exception {
        mockMvc.perform(get(uri))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/actuator/prometheus", "/actuator/info", "/actuator/env"})
    void 운영용_actuator는_로그인한_회원에게도_403(String uri) throws Exception {
        mockMvc.perform(get(uri).header("Authorization", bearer(jwtTokenProvider.createAccessToken(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void 서명을_고친_토큰은_401() throws Exception {
        String token = jwtTokenProvider.createAccessToken(1L);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        expectUnauthorized(bearer(tampered));
    }

    @Test
    void 다른_비밀키로_만든_토큰은_401() throws Exception {
        String forged = Jwts.builder()
                .subject("1")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("attacker-guessed-secret-key-at-least-32-bytes".getBytes(StandardCharsets.UTF_8)))
                .compact();

        expectUnauthorized(bearer(forged));
    }

    @Test
    void 만료된_토큰은_401() throws Exception {
        String expired = Jwts.builder()
                .subject("1")
                .expiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        expectUnauthorized(bearer(expired));
    }

    @Test
    void Bearer_없이_토큰만_보내면_401() throws Exception {
        expectUnauthorized(jwtTokenProvider.createAccessToken(1L));
    }

    private void expectUnauthorized(String authorizationHeader) throws Exception {
        mockMvc.perform(get("/api/members/me").header("Authorization", authorizationHeader))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 보안 단계에서 막히지 않았는지만 본다. 서비스가 가짜라 그 뒤의 응답(200·400·404 등)은 이 테스트의 관심사가 아니다.
     */
    private static ResultMatcher notBlockedBySecurity() {
        return result -> assertThat(result.getResponse().getStatus())
                .as("보안 설정에 막히면 안 되는 주소")
                .isNotIn(401, 403);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
