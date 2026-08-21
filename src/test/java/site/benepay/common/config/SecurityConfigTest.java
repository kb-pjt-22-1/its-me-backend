package site.benepay.common.config;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import site.benepay.auth.security.jwt.JwtProperties;
import site.benepay.auth.security.jwt.JwtTokenProvider;
import site.benepay.common.exception.JwtAuthenticationEntryPoint;

/**
 * Swagger 엔드포인트를 열어준 permitAll 규칙과, 나머지 요청은 여전히 인증을 요구하는
 * 기본 정책(anyRequest().authenticated())이 실제 필터 체인에서 그대로 동작하는지 검증한다.
 */
class SecurityConfigTest {

	private MockMvc mockMvc;

	@Configuration
	@EnableWebMvc
	@EnableWebSecurity
	static class TestConfig {

		@Bean
		JwtTokenProvider jwtTokenProvider() {
			// JwtTokenProvider.init()이 @PostConstruct(private)라 Mockito mock으로는
			// jwtProperties가 null인 채로 실행되어 NPE가 난다. 토큰을 실제로 만들 일은 없는
			// 테스트라 시크릿 값만 유효한 실제 인스턴스를 그대로 빈으로 등록한다.
			return new JwtTokenProvider(
				new JwtProperties("test-secret-key-at-least-32-bytes-long", "test-issuer", 600_000L, 3_600_000L));
		}

		@Bean
		StringRedisTemplate redisTemplate() {
			return mock(StringRedisTemplate.class);
		}

		@Bean
		JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
			return new JwtAuthenticationEntryPoint();
		}

		@Bean
		PingController pingController() {
			return new PingController();
		}
	}

	@RestController
	static class PingController {
		@GetMapping("/swagger-ui/index.html")
		public String swaggerUi() {
			return "ok";
		}

		@GetMapping("/v2/api-docs")
		public String apiDocs() {
			return "ok";
		}

		@GetMapping("/api/test/protected")
		public String protectedEndpoint() {
			return "ok";
		}
	}

	@BeforeEach
	void setUp() {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(TestConfig.class, SecurityConfig.class);
		context.refresh();

		mockMvc = MockMvcBuilders.webAppContextSetup(context)
			.apply(springSecurity())
			.build();
	}

	@Test
	void swaggerUiIsReachableWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
	}

	@Test
	void apiDocsIsReachableWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/v2/api-docs")).andExpect(status().isOk());
	}

	@Test
	void unrelatedEndpointsStillRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/test/protected")).andExpect(status().isUnauthorized());
	}

	@Test
	void unrelatedEndpointsSucceedOnceAuthenticated() throws Exception {
		mockMvc.perform(get("/api/test/protected").with(user("testuser"))).andExpect(status().isOk());
	}
}
