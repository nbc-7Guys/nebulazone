package nbc.chillguys.nebulazone.infra.security.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nebulazone.application.ban.sevice.BanService;

@Slf4j
@Component
@RequiredArgsConstructor
public class BanCheckFilter extends OncePerRequestFilter {

	private final BanService banService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null) {
			ip = request.getRemoteAddr();
		}

		if (banService.isBanned(ip)) {
			log.warn("[BAN] 차단된 IP: {}", ip);
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden IP");
			return;
		}

		filterChain.doFilter(request, response);
	}
}
