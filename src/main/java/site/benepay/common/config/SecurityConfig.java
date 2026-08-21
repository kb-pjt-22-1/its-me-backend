package site.benepay.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import site.benepay.auth.security.jwt.JwtAuthenticationFilter;
import site.benepay.auth.security.jwt.JwtTokenProvider;
import site.benepay.common.exception.JwtAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtTokenProvider jwtTokenProvider;
	private final StringRedisTemplate redisTemplate;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	public SecurityConfig(JwtTokenProvider jwtTokenProvider, StringRedisTemplate redisTemplate,
		JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.redisTemplate = redisTemplate;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					new AntPathRequestMatcher("/api/auth/logout")
				).authenticated()

				.requestMatchers(
					new AntPathRequestMatcher("/api/auth/**")
				).permitAll()

				.requestMatchers(
					new AntPathRequestMatcher(
						"/api/v1/webhooks/kb-card/**"
					)
				).permitAll()

				.requestMatchers(
					new AntPathRequestMatcher("/swagger-ui/**"),
					new AntPathRequestMatcher("/v2/api-docs"),
					new AntPathRequestMatcher("/swagger-resources/**")
				).permitAll()

				.anyRequest().authenticated()
			)
			.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, redisTemplate),
				UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
