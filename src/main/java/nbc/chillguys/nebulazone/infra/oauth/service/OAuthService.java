package nbc.chillguys.nebulazone.infra.oauth.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import nbc.chillguys.nebulazone.domain.user.dto.UserSignUpCommand;
import nbc.chillguys.nebulazone.domain.user.entity.OAuthType;
import nbc.chillguys.nebulazone.domain.user.entity.User;
import nbc.chillguys.nebulazone.domain.user.service.UserDomainService;
import nbc.chillguys.nebulazone.infra.oauth.dto.CustomOAuth2User;
import nbc.chillguys.nebulazone.infra.oauth.dto.OAuth2UserInfo;
import nbc.chillguys.nebulazone.infra.security.JwtUtil;
import nbc.chillguys.nebulazone.infra.security.dto.AuthTokens;

@Service
public class OAuthService extends DefaultOAuth2UserService {
	private final UserDomainService userDomainService;
	private final JwtUtil jwtUtil;
	private final Map<OAuthType, OAuth2UserInfoService> userInfoServiceMap;

	public OAuthService(UserDomainService userDomainService, JwtUtil jwtUtil,
		List<OAuth2UserInfoService> userInfoServices) {
		this.userDomainService = userDomainService;
		this.jwtUtil = jwtUtil;
		this.userInfoServiceMap = new EnumMap<>(OAuthType.class);
		userInfoServices.forEach(service ->
			userInfoServiceMap.put(service.getOAuthType(), service));
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		OAuth2UserInfoService userInfoService = userInfoServiceMap.get(OAuthType.from(registrationId));

		OAuth2User oAuth2User = super.loadUser(userRequest);
		Map<String, Object> attributes = oAuth2User.getAttributes();
		OAuth2UserInfo oAuth2UserInfo = userInfoService.parse(attributes);

		User user = getOrCreateUser(oAuth2UserInfo);

		AuthTokens authTokens = jwtUtil.generateTokens(user);

		return buildCustomUser(user, attributes, authTokens, userInfoService);
	}

	private User getOrCreateUser(OAuth2UserInfo oAuth2UserInfo) {
		if (userDomainService.validEmailWithOAuthType(oAuth2UserInfo.getEmail(), oAuth2UserInfo.getOAuthType())) {
			return userDomainService.findActiveUserByEmailAndOAuthType(oAuth2UserInfo.getEmail(),
				oAuth2UserInfo.getOAuthType());
		}

		userDomainService.validEmail(oAuth2UserInfo.getEmail());

		userDomainService.validNickname(oAuth2UserInfo.getNickname());

		return userDomainService.createUser(UserSignUpCommand.from(oAuth2UserInfo));
	}

	private CustomOAuth2User buildCustomUser(
		User user,
		Map<String, Object> attributes,
		AuthTokens tokens,
		OAuth2UserInfoService userInfoService
	) {
		return CustomOAuth2User.builder()
			.userId(user.getId())
			.roles(user.getRoles())
			.attributes(attributes)
			.nameAttributeKey(userInfoService.getUserNameAttributeKey())
			.accessToken(tokens.accessToken())
			.refreshToken(tokens.refreshToken())
			.build();
	}

}
