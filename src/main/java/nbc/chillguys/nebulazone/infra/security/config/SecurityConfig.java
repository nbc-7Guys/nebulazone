package nbc.chillguys.nebulazone.infra.security.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import nbc.chillguys.nebulazone.infra.oauth.handler.OAuth2SuccessHandler;
import nbc.chillguys.nebulazone.infra.oauth.service.OAuthService;
import nbc.chillguys.nebulazone.infra.security.JwtUtil;
import nbc.chillguys.nebulazone.infra.security.filter.BanCheckFilter;
import nbc.chillguys.nebulazone.infra.security.filter.CustomAuthenticationEntryPoint;
import nbc.chillguys.nebulazone.infra.security.filter.ExceptionLoggingFilter;
import nbc.chillguys.nebulazone.infra.security.filter.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	private final CustomAuthenticationEntryPoint entryPoint;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final OAuthService oAuthService;
	private final OAuth2SuccessHandler oAuth2SuccessHandler;
	private final ExceptionLoggingFilter exceptionLoggingFilter;
	private final BanCheckFilter banCheckFilter;

	public SecurityConfig(ObjectMapper objectMapper, JwtUtil jwtUtil, OAuthService oAuthService,
		OAuth2SuccessHandler oAuth2SuccessHandler, ExceptionLoggingFilter exceptionLoggingFilter,
		BanCheckFilter banCheckFilter) {
		this.entryPoint = new CustomAuthenticationEntryPoint(objectMapper);
		this.jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtUtil, entryPoint);
		this.oAuthService = oAuthService;
		this.oAuth2SuccessHandler = oAuth2SuccessHandler;
		this.exceptionLoggingFilter = exceptionLoggingFilter;
		this.banCheckFilter = banCheckFilter;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
		return httpSecurity
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/v3/api-docs/**",
					"/swagger-ui/**",
					"/swagger-ui.html",
					"/webjars/**",
					"/swagger-resources/**",
					"/favicon.ico",
					"/error",
					"/actuator/**",
					"/metrics/**",
					"/api/v1/**",
					"/ws/**",
					"/ws",
					"/chat/**",
					"/topic/**",
					"/internal/bans"
				).permitAll()
				.requestMatchers(
					"/auth/signin",
					"/users/signup",
					"/auth/reissue",
					"/oauth2/**"
				).permitAll()
				.requestMatchers("/admin/**").hasRole("ADMIN") // 어드민 전용
				.requestMatchers(
					HttpMethod.GET,
					"/auctions/**",
					"/catalogs/**",
					"/products/**"
				).permitAll()
				.anyRequest().authenticated())
			.oauth2Login(oauth2 -> oauth2
				.userInfoEndpoint(userInfo -> userInfo
					.userService(oAuthService))
				.successHandler(oAuth2SuccessHandler)
				.authorizationEndpoint(authorization -> authorization
					.authorizationRequestRepository(new HttpCookieOAuth2AuthorizationRequestRepository())
				)
			)
			.exceptionHandling(exception ->
				exception.authenticationEntryPoint(entryPoint))
			.addFilterBefore(banCheckFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(exceptionLoggingFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(
			"http://127.0.0.1:8080",    // 최종 배포 후 삭제
			"http://localhost:8080",    // 최종 배포 후 삭제
			"http://127.0.0.1:5173",    // 최종 배포 후 삭제
			"http://localhost:5173",    // 최종 배포 후 삭제
			"http://34.64.102.202:8080",    // 최종 배포 후 삭제
			"http://34.64.102.202:5173",    // 최종 배포 후 삭제
			"https://nebulazone-bz7n3o4r7-uguls-projects.vercel.app",
			"https://nebulazone-fe.vercel.app",
			"https://nebulazone.store",
			"https://www.nebulazone.store",
			"https://api2.nebulazone.store",
			"http://api2.nebulazone.store"
		));
		configuration.setAllowedMethods(List.of(
			"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
		));
		configuration.setAllowedHeaders(List.of(
			"Authorization",
			"Content-Type",
			"X-Requested-With"
		));
		configuration.setExposedHeaders(List.of(
			"X-Total-Count"
		));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
