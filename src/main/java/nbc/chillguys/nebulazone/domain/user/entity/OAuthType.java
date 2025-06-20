package nbc.chillguys.nebulazone.domain.user.entity;

import java.util.Arrays;

import lombok.Getter;
import nbc.chillguys.nebulazone.domain.user.exception.UserErrorCode;
import nbc.chillguys.nebulazone.domain.user.exception.UserException;

@Getter
public enum OAuthType {
	DOMAIN, KAKAO, NAVER;

	public static OAuthType from(String registrationId) {
		return Arrays.stream(OAuthType.values())
			.filter(r -> r.name().equalsIgnoreCase(registrationId))
			.findFirst()
			.orElseThrow(() -> new UserException(UserErrorCode.UNSUPPORTED_OAUTH_PROVIDER));
	}
}
