package nbc.chillguys.nebulazone.infra.security.dto;

public record AuthTokens(
	String accessToken,
	String refreshToken
) {
	public static AuthTokens of(String accessToken, String refreshToken) {
		return new AuthTokens(
			accessToken,
			refreshToken
		);
	}
}
