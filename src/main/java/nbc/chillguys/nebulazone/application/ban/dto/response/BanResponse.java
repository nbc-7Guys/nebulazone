package nbc.chillguys.nebulazone.application.ban.dto.response;

import java.time.LocalDateTime;

import nbc.chillguys.nebulazone.domain.ban.dto.BanInfo;

public record BanResponse(
	Long id,
	String ipAddress,
	String reason,
	String attackType,
	LocalDateTime createdAt,
	LocalDateTime expiresAt
) {
	public static BanResponse from(BanInfo info) {
		return new BanResponse(
			info.id(),
			info.ipAddress(),
			info.reason(),
			info.attackType(),
			info.createdAt(),
			info.expiresAt()
		);
	}
}
