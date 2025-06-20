package nbc.chillguys.nebulazone.infra.redis.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisBanService {

	private static final String BAN_PREFIX = "ban:";
	private final RedisTemplate<String, Object> redisTemplate;

	/**
	 * 주어진 IP 주소를 일정 시간 동안 밴 처리
	 *
	 * @param ipAddress 밴 처리할 클라이언트의 IP 주소
	 * @param seconds 밴 유지 시간 (초 단위)
	 * @author 정석현
	 */
	public void registerBan(String ipAddress, long seconds) {
		redisTemplate.opsForValue().set(BAN_PREFIX + ipAddress, true, Duration.ofSeconds(seconds));
	}

	/**
	 * 해당 IP 주소가 현재 밴 상태인지 확인
	 *
	 * @param ipAddress 확인할 클라이언트의 IP 주소
	 * @return 밴 상태이면 {@code true}, 아니면 {@code false}
	 * @author 정석현
	 */
	public boolean isBanned(String ipAddress) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(BAN_PREFIX + ipAddress));
	}

	/**
	 * 해당 IP 주소의 밴 상태를 해제
	 *
	 * @param ipAddress 밴을 해제할 클라이언트의 IP 주소
	 * @author 정석현
	 */
	public void unban(String ipAddress) {
		redisTemplate.delete(BAN_PREFIX + ipAddress);
	}
}
