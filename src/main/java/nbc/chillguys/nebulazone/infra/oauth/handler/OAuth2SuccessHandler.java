package nbc.chillguys.nebulazone.infra.oauth.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.infra.oauth.dto.CustomOAuth2User;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
	@Value("${frontend.url}")
	private String frontendUrl;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {
		CustomOAuth2User oAuth2User = (CustomOAuth2User)authentication.getPrincipal();

		oAuth2User.attributes().get("authorities");

		response.setHeader("Authorization", "Bearer " + oAuth2User.accessToken());
		Cookie cookie = new Cookie("Refresh_Token", oAuth2User.refreshToken());
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(2 * 24 * 60 * 60);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setStatus(HttpServletResponse.SC_OK);

		String redirectUrl = frontendUrl + "/oauth/redirect"
			+ "?access_token=" + oAuth2User.accessToken()
			+ "&refresh_token=" + oAuth2User.refreshToken();
		getRedirectStrategy().sendRedirect(request, response, redirectUrl);
	}
}
