package nbc.chillguys.nebulazone.domain.ban.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import nbc.chillguys.nebulazone.domain.ban.entity.Ban;

public interface BanRepository extends JpaRepository<Ban, Long>, BanRepositoryCustom {
	void deleteByIpAddress(String ipAddress);

	Optional<Ban> findByIpAddress(String ipAddress);
}
