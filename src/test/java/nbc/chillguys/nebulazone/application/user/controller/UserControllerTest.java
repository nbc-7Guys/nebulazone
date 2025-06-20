package nbc.chillguys.nebulazone.application.user.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import nbc.chillguys.nebulazone.application.user.dto.request.SignUpUserRequest;
import nbc.chillguys.nebulazone.application.user.dto.request.UpdateUserRequest;
import nbc.chillguys.nebulazone.application.user.dto.request.WithdrawUserRequest;
import nbc.chillguys.nebulazone.application.user.dto.response.UserResponse;
import nbc.chillguys.nebulazone.application.user.dto.response.WithdrawUserResponse;
import nbc.chillguys.nebulazone.application.user.service.UserService;
import nbc.chillguys.nebulazone.config.TestSecurityConfig;
import nbc.chillguys.nebulazone.domain.user.entity.OAuthType;
import nbc.chillguys.nebulazone.support.MockMvc.TestMockConfig;
import nbc.chillguys.nebulazone.support.mockuser.WithCustomMockUser;

@DisplayName("유저 컨트롤러 단위 테스트")
@Import({TestSecurityConfig.class, TestMockConfig.class})
@WebMvcTest(UserController.class)
class UserControllerTest {
	@MockitoBean
	private UserService userService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private final LocalDateTime now = LocalDateTime.now();

	private final UserResponse userResponse = new UserResponse(
		1L,
		"test@test.com",
		"01012345678",
		"test",
		"test_profile_image_url",
		0,
		OAuthType.DOMAIN,
		null,
		Set.of(new UserResponse.AddressResponse(
			"test_road_address",
			"test_detail_address",
			"address_nickname"
		)),
		now,
		now
	);

	@Test
	@DisplayName("회원 가입 성공")
	void success_signUp() throws Exception {
		// Given
		SignUpUserRequest request = new SignUpUserRequest("test@test.com", "testPassword1!",
			"01012345678", "test",
			Set.of(new SignUpUserRequest.SignUpUserAddressRequest("test_road_address", "test_detail_address",
				"address_nickname")));

		given(userService.signUp(any()))
			.willReturn(userResponse);

		// When
		ResultActions perform = mockMvc.perform(multipart("/users/signup")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// Then
		perform.andDo(print())
			.andExpectAll(
				status().isCreated(),
				jsonPath("$.userId")
					.value(1L),
				jsonPath("$.email")
					.value("test@test.com"),
				jsonPath("$.phone")
					.value("01012345678"),
				jsonPath("$.nickname")
					.value("test"),
				jsonPath("$.profileImageUrl")
					.value("test_profile_image_url"),
				jsonPath("$.point")
					.value(0),
				jsonPath("$.oAuthType")
					.value("DOMAIN"),
				jsonPath("$.addresses[0].addressNickname")
					.value("address_nickname"),
				jsonPath("$.addresses[0].roadAddress")
					.value("test_road_address"),
				jsonPath("$.addresses[0].detailAddress")
					.value("test_detail_address"),
				jsonPath("$.createdAt")
					.value(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
				jsonPath("$.modifiedAt")
					.value(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
			);

		verify(userService, times(1)).signUp(any());

	}

	@Test
	@DisplayName("유저 조회 성공")
	void success_getUser() throws Exception {
		// Given
		given(userService.getUser(anyLong()))
			.willReturn(userResponse);

		// When
		ResultActions perform = mockMvc.perform(get("/users/{userId}", 1L));

		// Then
		perform.andDo(print())
			.andExpectAll(
				status().isOk(),
				jsonPath("$.userId")
					.value(1L),
				jsonPath("$.email")
					.value("test@test.com"),
				jsonPath("$.phone")
					.value("01012345678"),
				jsonPath("$.nickname")
					.value("test"),
				jsonPath("$.profileImageUrl")
					.value("test_profile_image_url"),
				jsonPath("$.point")
					.value(0),
				jsonPath("$.oAuthType")
					.value("DOMAIN"),
				jsonPath("$.addresses[0].addressNickname")
					.value("address_nickname"),
				jsonPath("$.addresses[0].roadAddress")
					.value("test_road_address"),
				jsonPath("$.addresses[0].detailAddress")
					.value("test_detail_address"),
				jsonPath("$.createdAt")
					.value(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
				jsonPath("$.modifiedAt")
					.value(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
			);

		verify(userService, times(1)).getUser(anyLong());

	}

	@Test
	@WithCustomMockUser
	@DisplayName("유저 닉네임, 비밀번호 수정 성공")
	void success_updateUserNicknameOrPassword() throws Exception {
		// Given
		UpdateUserRequest request = new UpdateUserRequest("newNickname",
			new UpdateUserRequest.PasswordChangeForm("encodedPassword1!", "newPassword1!"));

		given(userService.updateUserNicknameOrPassword(any(), any()))
			.willReturn(userResponse);

		// When
		ResultActions perform = mockMvc.perform(patch("/users")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request))
		);

		// Then
		perform.andDo(print())
			.andExpectAll(
				status().isOk(),
				jsonPath("$.userId")
					.value(1L),
				jsonPath("$.email")
					.value("test@test.com"),
				jsonPath("$.phone")
					.value("01012345678"),
				jsonPath("$.nickname")
					.value("test"),
				jsonPath("$.profileImageUrl")
					.value("test_profile_image_url"),
				jsonPath("$.point")
					.value(0),
				jsonPath("$.oAuthType")
					.value("DOMAIN"),
				jsonPath("$.addresses[0].addressNickname")
					.value("address_nickname"),
				jsonPath("$.addresses[0].roadAddress")
					.value("test_road_address"),
				jsonPath("$.addresses[0].detailAddress")
					.value("test_detail_address"),
				jsonPath("$.createdAt")
					.value(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
				jsonPath("$.modifiedAt")
					.value(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
			);

		verify(userService, times(1)).updateUserNicknameOrPassword(any(), any());

	}

	@Test
	@WithCustomMockUser
	@DisplayName("유저 프로필 이미지 수정 성공")
	void success_updateUserProfileImage() throws Exception {
		// Given
		byte[] dummyJpegBytes = new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, 0x00, 0x00};

		MockMultipartFile imagePart = new MockMultipartFile(
			"profileImage",
			"profile.jpg",
			MediaType.IMAGE_JPEG_VALUE,
			dummyJpegBytes
		);

		given(userService.updateUserProfileImage(any(), any()))
			.willReturn(userResponse);

		// When
		ResultActions perform = mockMvc.perform(multipart("/users")
			.file(imagePart)
			.with(r -> {
				r.setMethod(HttpMethod.PUT.name());
				return r;
			}));

		// Then
		perform.andDo(print())
			.andExpectAll(
				status().isOk(),
				jsonPath("$.userId")
					.value(1L),
				jsonPath("$.email")
					.value("test@test.com"),
				jsonPath("$.phone")
					.value("01012345678"),
				jsonPath("$.nickname")
					.value("test"),
				jsonPath("$.profileImageUrl")
					.value("test_profile_image_url"),
				jsonPath("$.point")
					.value(0),
				jsonPath("$.oAuthType")
					.value("DOMAIN"),
				jsonPath("$.addresses[0].addressNickname")
					.value("address_nickname"),
				jsonPath("$.addresses[0].roadAddress")
					.value("test_road_address"),
				jsonPath("$.addresses[0].detailAddress")
					.value("test_detail_address"),
				jsonPath("$.createdAt")
					.value(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
				jsonPath("$.modifiedAt")
					.value(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
			);

	}

	@Test
	@WithCustomMockUser
	@DisplayName("회원 탈퇴 성공")
	void success_withdrawUser() throws Exception {
		// Given
		WithdrawUserRequest request = new WithdrawUserRequest("encodedPassword1!");
		WithdrawUserResponse response = new WithdrawUserResponse(1L);

		given(userService.withdrawUser(any(), any()))
			.willReturn(response);

		// When
		ResultActions perform = mockMvc.perform(delete("/users")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// Then
		perform.andDo(print())
			.andExpectAll(
				status().isOk(),
				jsonPath("$.userId")
					.value(1L)
			);

		verify(userService, times(1)).withdrawUser(any(), any());

	}

}
