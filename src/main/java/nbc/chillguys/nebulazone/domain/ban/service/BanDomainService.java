package nbc.chillguys.nebulazone.domain.ban.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.domain.ban.dto.BanCreateCommand;
import nbc.chillguys.nebulazone.domain.ban.entity.Ban;
import nbc.chillguys.nebulazone.domain.ban.repository.BanRepository;

@Service
@RequiredArgsConstructor
public class BanDomainService {
	private final BanRepository banRepository;

	/**
	 * 밴 생성 로직
	 *
	 * @param command 밴 생성
	 * @author 정석현
	 */
	@Transactional
	public void createBan(BanCreateCommand command) {
		if (isBanned(command.ipAddress())) {
			return;
		}
		Ban ban = Ban.create(command);
		banRepository.save(ban);
	}

	/**
	 * 밴 유무
	 * @param ipAddress 아이피 주소
	 * @return 밴 여부
	 * @author 정석현
	 */
	public boolean isBanned(String ipAddress) {
		return banRepository.findActiveBanByIp(ipAddress).isPresent();
	}

}
