package nbc.chillguys.nebulazone.infra.security.filter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import nbc.chillguys.nebulazone.domain.user.entity.User;

@Component
public class ExceptionLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(ExceptionLoggingFilter.class);

	private static final Set<String> EXCLUDED_PATHS = Set.of(
		"/actuator/prometheus", "/actuator/health", "/actuator/info",
		"/ws/**", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**"
	);
	private static final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Override
	protected void doFilterInternal(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull FilterChain filterChain
	) throws ServletException, IOException {

		String uri = request.getRequestURI();
		if (EXCLUDED_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri))) {
			filterChain.doFilter(request, response);
			return;
		}

		String ip = getClientIP(request);
		String method = request.getMethod();
		String traceId = UUID.randomUUID().toString().replace("-", "");
		String userAgent = request.getHeader("User-Agent");
		String referer = request.getHeader("Referer");

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()
			&& !"anonymousUser".equals(authentication.getPrincipal())) {

			Object principal = authentication.getPrincipal();
			if (principal instanceof User user) {
				MDC.put("userId", String.valueOf(user.getId()));
				MDC.put("userEmail", user.getEmail());
			} else {
				MDC.put("userId", principal.toString());
				MDC.put("userEmail", "");
			}
		} else {
			MDC.put("userId", "비회원");
			MDC.put("userEmail", "");
		}

		MDC.put("ip", ip);
		MDC.put("uri", uri);
		MDC.put("method", method);
		MDC.put("traceId", traceId);
		MDC.put("userAgent", userAgent != null ? userAgent : "");
		MDC.put("referer", referer != null ? referer : "");

		log.info("[REQUEST] {} {} | IP={} | traceId={} | userId={} | userEmail={}",
			method, uri, ip, traceId, MDC.get("userId"), MDC.get("userEmail"));

		try {
			filterChain.doFilter(request, response);
		} catch (Exception e) {
			log.error("[FILTER ERROR] {} {} | IP={} | traceId={} | userId={} | userEmail={} | message={}",
				method, uri, ip, traceId, MDC.get("userId"), MDC.get("userEmail"), e.getMessage(), e);
			throw e;
		} finally {
			log.info("[RESPONSE] {} {} | status={} | IP={} | traceId={} | userId={} | userEmail={}",
				method, uri, response.getStatus(), ip, traceId, MDC.get("userId"), MDC.get("userEmail"));
			MDC.clear();
		}
	}

	private String getClientIP(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		if (ip != null && ip.contains(",")) {
			ip = ip.split(",")[0];
		}
		return ip;
	}
}
