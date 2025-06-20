package nbc.chillguys.nebulazone.domain.ban.dto;

import java.time.LocalDateTime;

import nbc.chillguys.nebulazone.application.ban.dto.request.BanCreateRequest;

public record BanCreateCommand(
	String ipAddress,
	String attackType,
	String reason,
	LocalDateTime expiresAt
) {
	public static BanCreateCommand from(BanCreateRequest request) {
		return new BanCreateCommand(
			request.ipAddress(),
			request.attackType(),
			request.reason(),
			request.expiresAt()
		);
	}
}
