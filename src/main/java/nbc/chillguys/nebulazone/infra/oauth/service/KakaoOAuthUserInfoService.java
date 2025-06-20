package nbc.chillguys.nebulazone.infra.oauth.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.domain.user.entity.OAuthType;
import nbc.chillguys.nebulazone.infra.oauth.dto.KakaoOAuth2UserInfo;
import nbc.chillguys.nebulazone.infra.oauth.dto.OAuth2UserInfo;

@Service
@RequiredArgsConstructor
public class KakaoOAuthUserInfoService implements OAuth2UserInfoService {
	private final ObjectMapper objectMapper;

	@Override
	public OAuth2UserInfo parse(Map<String, Object> attributes) {
		return objectMapper.convertValue(attributes, KakaoOAuth2UserInfo.class);
	}

	@Override
	public OAuthType getOAuthType() {
		return OAuthType.KAKAO;
	}

	@Override
	public String getUserNameAttributeKey() {
		return "id";
	}

}
