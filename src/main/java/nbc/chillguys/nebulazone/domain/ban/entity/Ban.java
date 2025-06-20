package nbc.chillguys.nebulazone.domain.ban.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nbc.chillguys.nebulazone.domain.ban.dto.BanCreateCommand;

@Entity
@Getter
@Table(name = "bans")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ban {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ban_id")
	private Long id;

	@Column(name = "ip_address", nullable = false, length = 45)
	private String ipAddress;

	@Column(name = "attack_type", length = 50)
	private String attackType;

	@Column(name = "reason", columnDefinition = "TEXT")
	private String reason;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	public static Ban create(BanCreateCommand command) {
		Ban ban = new Ban();
		ban.ipAddress = command.ipAddress();
		ban.attackType = command.attackType();
		ban.reason = command.reason();
		ban.expiresAt = command.expiresAt();
		return ban;
	}

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}

	public boolean isExpired() {
		return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
	}

	public boolean isPermanent() {
		return expiresAt == null;
	}

	public boolean isActive() {
		return !isExpired();
	}
}
