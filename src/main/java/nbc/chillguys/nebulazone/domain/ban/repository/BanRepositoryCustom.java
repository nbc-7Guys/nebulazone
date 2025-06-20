package nbc.chillguys.nebulazone.domain.ban.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import nbc.chillguys.nebulazone.domain.ban.entity.Ban;

public interface BanRepositoryCustom {
	Optional<Ban> findActiveBanByIp(String ip);

	void deleteAllExpired(LocalDateTime now);
}
