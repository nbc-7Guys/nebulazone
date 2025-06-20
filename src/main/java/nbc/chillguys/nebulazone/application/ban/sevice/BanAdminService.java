package nbc.chillguys.nebulazone.application.ban.sevice;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.application.ban.dto.response.BanResponse;
import nbc.chillguys.nebulazone.domain.ban.service.BanAdminDomainService;
import nbc.chillguys.nebulazone.infra.redis.service.RedisBanService;

@Service
@RequiredArgsConstructor
public class BanAdminService {

	private final BanAdminDomainService banAdminDomainService;
	private final RedisBanService redisBanService;

	public void unban(String ipAddress) {
		redisBanService.unban(ipAddress);
		banAdminDomainService.unban(ipAddress);
	}

	public List<BanResponse> findBans() {
		return banAdminDomainService.findBanInfos().stream()
			.map(BanResponse::from)
			.toList();
	}

	public boolean isBanned(String ipAddress) {
		return redisBanService.isBanned(ipAddress);
	}
}
