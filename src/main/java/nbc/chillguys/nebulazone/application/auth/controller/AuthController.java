package nbc.chillguys.nebulazone.application.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.application.auth.dto.request.SignInRequest;
import nbc.chillguys.nebulazone.application.auth.dto.response.ReissueResponse;
import nbc.chillguys.nebulazone.application.auth.dto.response.SignInResponse;
import nbc.chillguys.nebulazone.application.auth.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;

	@PostMapping("/signin")
	public ResponseEntity<SignInResponse> signIn(
		@Valid @RequestBody SignInRequest signInRequestDto,
		HttpServletResponse httpServletResponse
	) {
		SignInResponse response = authService.signIn(signInRequestDto);

		Cookie cookie = new Cookie("Refresh_Token", response.refreshToken());
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(2 * 24 * 60 * 60);

		httpServletResponse.addCookie(cookie);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/signout")
	public ResponseEntity<String> signOut(HttpServletResponse httpServletResponse) {
		authService.signOut();

		Cookie cookie = new Cookie("Refresh_Token", null);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(0);

		httpServletResponse.addCookie(cookie);

		return ResponseEntity.ok("로그아웃 성공");
	}

	@PostMapping("/reissue")
	public ResponseEntity<ReissueResponse> reissueAccessToken(
		@CookieValue("Refresh_Token") String refreshToken
	) {
		ReissueResponse response = authService.reissueAccessToken(refreshToken);

		return ResponseEntity.ok(response);
	}
}
