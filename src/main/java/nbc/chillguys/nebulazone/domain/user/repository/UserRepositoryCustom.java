package nbc.chillguys.nebulazone.domain.user.repository;

import java.util.Optional;

import nbc.chillguys.nebulazone.domain.user.entity.OAuthType;
import nbc.chillguys.nebulazone.domain.user.entity.User;

public interface UserRepositoryCustom {
	Optional<User> findActiveUserByEmail(String email);

	Optional<User> findActiveUserById(Long userId);

	Optional<User> findActiveUserByEmailAndOAuthType(String email, OAuthType oAuthType);

	Optional<User> findUserById(Long userId);

	boolean existsByEmailAndOAuthType(String email, OAuthType oAuthType);
}
