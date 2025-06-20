package nbc.chillguys.nebulazone.application.ban.sevice;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.application.ban.dto.request.BanCreateRequest;
import nbc.chillguys.nebulazone.domain.ban.dto.BanCreateCommand;
import nbc.chillguys.nebulazone.domain.ban.service.BanDomainService;
import nbc.chillguys.nebulazone.infra.redis.service.RedisBanService;

@Service
@RequiredArgsConstructor
public class BanService {

	private final BanDomainService banDomainService;
	private final RedisBanService redisBanService;

	public void createBan(BanCreateRequest request) {
		if (banDomainService.isBanned(request.ipAddress())) {
			return;
		}

		BanCreateCommand command = BanCreateCommand.from(request);
		banDomainService.createBan(command);

		long ttlSeconds;

		if (command.expiresAt() != null) {
			LocalDateTime now = LocalDateTime.now();
			ttlSeconds = Math.max(0, Duration.between(now, command.expiresAt()).getSeconds());
		} else {
			ttlSeconds = Duration.ofDays(3650).getSeconds();
		}

		redisBanService.registerBan(command.ipAddress(), ttlSeconds);
	}

	public boolean isBanned(String ipAddress) {
		return redisBanService.isBanned(ipAddress);
	}
}
