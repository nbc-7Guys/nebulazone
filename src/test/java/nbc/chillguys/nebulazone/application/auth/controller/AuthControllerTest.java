package nbc.chillguys.nebulazone.application.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import nbc.chillguys.nebulazone.application.auth.dto.request.SignInRequest;
import nbc.chillguys.nebulazone.application.auth.dto.response.ReissueResponse;
import nbc.chillguys.nebulazone.application.auth.dto.response.SignInResponse;
import nbc.chillguys.nebulazone.application.auth.service.AuthService;
import nbc.chillguys.nebulazone.config.TestSecurityConfig;
import nbc.chillguys.nebulazone.support.MockMvc.TestMockConfig;
import nbc.chillguys.nebulazone.support.mockuser.WithCustomMockUser;

@DisplayName("auth 컨트롤러 단위 테스트")
@Import({TestSecurityConfig.class, TestMockConfig.class})
@WebMvcTest(AuthController.class)
class AuthControllerTest {
	@MockitoBean
	private AuthService authService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("로그인 성공")
	void success_signIn() throws Exception {
		// Given
		SignInRequest request = new SignInRequest("test@test.com", "encodedPassword1!");
		SignInResponse response = new SignInResponse("test_access_token", "test_refresh_token");

		given(authService.signIn(any()))
			.willReturn(response);

		// When
		ResultActions perform = mockMvc.perform(post("/auth/signin")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// Then
		perform.andDo(print())
			.andExpectAll(
				status().isOk(),
				jsonPath("$.accessToken").value("test_access_token"),
				cookie().exists("Refresh_Token"),
				cookie().value("Refresh_Token", "test_refresh_token"),
				cookie().httpOnly("Refresh_Token", true)
				// cookie().secure("Refresh_Token", true) // HTTPS 설정 시 추가
			);

	}

	@Test
	@WithCustomMockUser
	@DisplayName("로그 아웃 성공")
	void success_signOut() throws Exception {
		// Given

		// When
		ResultActions perform = mockMvc.perform(post("/auth/signout"));

		// Then
		perform.andDo(print())
			.andExpectAll(
				status().isOk(),
				jsonPath("$")
					.value("로그아웃 성공"),
				cookie().exists("Refresh_Token"),
				cookie().maxAge("Refresh_Token", 0)
			);

	}

	@Test
	@DisplayName("access token 재발급 성공")
	void success_reissueAccessToken() throws Exception {
		// Given
		ReissueResponse response = new ReissueResponse("regenerateAccessToken");

		given(authService.reissueAccessToken(anyString()))
			.willReturn(response);

		// When
		ResultActions perform = mockMvc.perform(post("/auth/reissue")
			.cookie(new Cookie("Refresh_Token", "test_refresh_token")));

		// Then
		perform.andDo(print())
			.andExpectAll(
				status().isOk(),
				jsonPath("$.accessToken")
					.value("regenerateAccessToken")
			);
	}
}
