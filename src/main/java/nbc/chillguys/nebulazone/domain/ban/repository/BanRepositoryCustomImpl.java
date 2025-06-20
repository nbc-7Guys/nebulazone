package nbc.chillguys.nebulazone.domain.ban.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.domain.ban.entity.Ban;
import nbc.chillguys.nebulazone.domain.ban.entity.QBan;

@Repository
@RequiredArgsConstructor
public class BanRepositoryCustomImpl implements BanRepositoryCustom {
	private final JPAQueryFactory queryFactory;

	@Override
	public Optional<Ban> findActiveBanByIp(String ip) {
		QBan ban = QBan.ban;
		return Optional.ofNullable(queryFactory.selectFrom(ban)
			.where(
				ban.ipAddress.eq(ip),
				ban.expiresAt.isNull().or(ban.expiresAt.after(LocalDateTime.now()))
			)
			.fetchFirst());
	}

	@Override
	public void deleteAllExpired(LocalDateTime now) {
		QBan ban = QBan.ban;
		queryFactory.delete(ban)
			.where(ban.expiresAt.isNotNull().and(ban.expiresAt.loe(now)))
			.execute();
	}
}
