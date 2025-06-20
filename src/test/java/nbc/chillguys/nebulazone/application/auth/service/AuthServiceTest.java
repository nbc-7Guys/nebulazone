package nbc.chillguys.nebulazone.application.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import nbc.chillguys.nebulazone.application.auth.dto.request.SignInRequest;
import nbc.chillguys.nebulazone.application.auth.dto.response.SignInResponse;
import nbc.chillguys.nebulazone.domain.user.entity.Address;
import nbc.chillguys.nebulazone.domain.user.entity.OAuthType;
import nbc.chillguys.nebulazone.domain.user.entity.User;
import nbc.chillguys.nebulazone.domain.user.entity.UserRole;
import nbc.chillguys.nebulazone.domain.user.service.UserDomainService;
import nbc.chillguys.nebulazone.infra.security.JwtUtil;
import nbc.chillguys.nebulazone.infra.security.dto.AuthTokens;

@DisplayName("auth 어플리케이션 서비스 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
	@Mock
	private UserDomainService userDomainService;

	@Mock
	private JwtUtil jwtUtil;

	@InjectMocks
	private AuthService authService;

	@Spy
	private User user;

	@BeforeEach
	void init() {
		user = User.builder()
			.email("test@test.com")
			.password("encodedPassword")
			.phone("01012345678")
			.nickname("test")
			.profileImage("test_profile_image_url")
			.point(0)
			.oAuthType(OAuthType.DOMAIN)
			.roles(Set.of(UserRole.ROLE_USER))
			.addresses(Set.of(Address.builder()
				.addressNickname("test_address_nickname")
				.roadAddress("test_road_address")
				.detailAddress("test_detail_address")
				.build()))
			.build();

		ReflectionTestUtils.setField(user, "id", 1L);
	}

	@Test
	@DisplayName("로그인 성공")
	void success_signIn() {
		// Given
		SignInRequest request = new SignInRequest("test@test.com", "encodedPassword1!");
		AuthTokens authTokens = new AuthTokens("test_access_token", "test_refresh_token");

		given(userDomainService.findActiveUserByEmail(anyString()))
			.willReturn(user);
		given(jwtUtil.generateTokens(any(User.class)))
			.willReturn(authTokens);

		// When
		SignInResponse response = authService.signIn(request);

		// Then
		verify(userDomainService, times(1)).findActiveUserByEmail(anyString());
		verify(userDomainService, times(1)).validPassword(anyString(), anyString());
		verify(jwtUtil, times(1)).generateTokens(any(User.class));

		assertThat(response)
			.isNotNull();
		assertThat(response.accessToken())
			.isEqualTo("test_access_token");
		assertThat(response.refreshToken())
			.isEqualTo("test_refresh_token");

	}

	@Test
	@DisplayName("로그아웃 성공")
	void success_signOut() {
		// Given
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken("user", "password");
		SecurityContextHolder.getContext().setAuthentication(authentication);

		// When
		authService.signOut();

		// Then
		assertThat(SecurityContextHolder.getContext().getAuthentication())
			.isNull();

	}
}
