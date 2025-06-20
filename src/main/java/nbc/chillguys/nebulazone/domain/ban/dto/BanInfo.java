package nbc.chillguys.nebulazone.domain.ban.dto;

import java.time.LocalDateTime;

import nbc.chillguys.nebulazone.domain.ban.entity.Ban;

public record BanInfo(
	Long id,
	String ipAddress,
	String reason,
	String attackType,
	LocalDateTime createdAt,
	LocalDateTime expiresAt
) {

	public static BanInfo from(final Ban ban) {
		return new BanInfo(
			ban.getId(),
			ban.getIpAddress(),
			ban.getReason(),
			ban.getAttackType(),
			ban.getCreatedAt(),
			ban.getExpiresAt()
		);
	}
}
