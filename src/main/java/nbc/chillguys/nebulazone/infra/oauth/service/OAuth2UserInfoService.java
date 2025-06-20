package nbc.chillguys.nebulazone.infra.oauth.service;

import java.util.Map;

import nbc.chillguys.nebulazone.domain.user.entity.OAuthType;
import nbc.chillguys.nebulazone.infra.oauth.dto.OAuth2UserInfo;

public interface OAuth2UserInfoService {
	OAuth2UserInfo parse(Map<String, Object> attributes);

	OAuthType getOAuthType();

	String getUserNameAttributeKey();
}
