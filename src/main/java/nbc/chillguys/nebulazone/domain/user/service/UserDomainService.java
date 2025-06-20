package nbc.chillguys.nebulazone.domain.user.service;

import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.domain.user.dto.UserPointChargeCommand;
import nbc.chillguys.nebulazone.domain.user.dto.UserSignUpCommand;
import nbc.chillguys.nebulazone.domain.user.dto.UserUpdateCommand;
import nbc.chillguys.nebulazone.domain.user.entity.OAuthType;
import nbc.chillguys.nebulazone.domain.user.entity.User;
import nbc.chillguys.nebulazone.domain.user.entity.UserRole;
import nbc.chillguys.nebulazone.domain.user.exception.UserErrorCode;
import nbc.chillguys.nebulazone.domain.user.exception.UserException;
import nbc.chillguys.nebulazone.domain.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDomainService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 탈퇴되지 않은 유저를 email로 조회<br>
	 * 조회 시 roles도 함께 fetch
	 * @param email 이메일
	 * @throws UserException 없을 시 예외 발생
	 * @return user
	 * @author 이승현
	 */
	public User findActiveUserByEmail(String email) {
		return userRepository.findActiveUserByEmail(email)
			.orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
	}

	/**
	 * 비밀번호 검증
	 * @param rawPassword 원본 비밀번호
	 * @param encodedPassword 암호화된 비밀번호
	 * @throws UserException 일치 하지 않을 시 예외 발생
	 * @author 이승현
	 */
	public void validPassword(String rawPassword, String encodedPassword) {
		if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
			throw new UserException(UserErrorCode.WRONG_PASSWORD);
		}
	}

	/**
	 * 탈퇴되지 않은 유저를 id로 조회<br>
	 * 조회 시 address도 함께 fetch
	 * @param userId 유저 id
	 * @throws UserException 없을 시 예외 발생
	 * @return user
	 * @author 이승현
	 */
	public User findActiveUserById(Long userId) {
		return userRepository.findActiveUserById(userId)
			.orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
	}

	/**
	 * 유저 생성<br>
	 * domain 가입 유형
	 * @param userSignUpCommand 유저 가입 정보
	 * @return user
	 * @author 이승현
	 */
	@Transactional
	public User createUser(UserSignUpCommand userSignUpCommand) {
		String phone = null;
		if (StringUtils.hasText(userSignUpCommand.phone())) {
			phone = userSignUpCommand.phone().replaceAll("-", "");
		}

		String password = null;
		if (StringUtils.hasText(userSignUpCommand.password())) {
			password = passwordEncoder.encode(userSignUpCommand.password());
		}

		User user = User.builder()
			.email(userSignUpCommand.email())
			.password(password)
			.phone(phone)
			.nickname(userSignUpCommand.nickname())
			.profileImage(userSignUpCommand.profileImageUrl())
			.point(0)
			.oAuthType(userSignUpCommand.oAuthType())
			.oAuthId(userSignUpCommand.oauthId())
			.roles(Set.of(UserRole.ROLE_USER))
			.addresses(userSignUpCommand.addresses())
			.build();

		return userRepository.save(user);
	}

	/**
	 * 이메일 검증
	 * @param email 이메일
	 * @throws UserException 중복된 이메일이 존재할 시 예외 발생
	 * @author 이승현
	 */
	public void validEmail(String email) {
		if (userRepository.existsByEmail(email)) {
			throw new UserException(UserErrorCode.ALREADY_EXISTS_EMAIL);
		}
	}

	/**
	 * 닉네임 검증
	 * @param nickname 닉네임
	 * @throws UserException 중복된 닉네임 존재할 시 예외 발생
	 * @author 이승현
	 */
	public void validNickname(String nickname) {
		if (userRepository.existsByNickname(nickname)) {
			throw new UserException(UserErrorCode.ALREADY_EXISTS_NICKNAME);
		}
	}

	/**
	 * 프로필 이미지 수정
	 * @param profileImageUrl 프로필 이미지 url
	 * @param user 유저
	 * @author 이승현
	 */
	@Transactional
	public void updateUserProfileImage(String profileImageUrl, User user) {
		user.updateProfileImage(profileImageUrl);
	}

	/**
	 * 새 비밀번호 검증
	 * @param newPassword 새 비밀번호
	 * @param encodedPassword 암호화된 비밀번호
	 * @author 이승현
	 */
	public void validNewPassword(String newPassword, String encodedPassword) {
		if (passwordEncoder.matches(newPassword, encodedPassword)) {
			throw new UserException(UserErrorCode.SAME_PASSWORD);
		}
	}

	/**
	 * 회원 탈퇴
	 * @param user 유저
	 * @author 이승현
	 */
	@Transactional
	public void withdrawUser(User user) {
		user.withdraw();
	}

	/**
	 * 소셜 로그인 시 해당 provider로 존재하는 user 조회
	 * @param email 이메일
	 * @param oAuthType 소셜 로그인 타입
	 * @return user
	 * @author 이승현
	 */
	public User findActiveUserByEmailAndOAuthType(String email, OAuthType oAuthType) {
		return userRepository.findActiveUserByEmailAndOAuthType(email, oAuthType)
			.orElseThrow(() -> new UserException(UserErrorCode.ALREADY_EXISTS_EMAIL));
	}

	/**
	 * 포인 환전시 포인트가 충분한지 조회
	 *
	 * @param user 유저
	 * @param price 요청 포인트
	 * @author 정석현
	 */
	public void validEnoughPoint(User user, Long price) {
		if (user.hasNotEnoughPoint(price)) {
			throw new UserException(UserErrorCode.INSUFFICIENT_BALANCE);
		}
	}

	/**
	 * 닉네임 또는 비밀번호 수정
	 * @param userUpdateCommand 유저 수정(userId, nickname, oldPassword, newPassword)
	 * @return user
	 * @author 이승현
	 */
	@Transactional
	public User updateUserNicknameOrPassword(UserUpdateCommand userUpdateCommand) {
		User user = userUpdateCommand.user();

		if (userUpdateCommand.nickname() == null && userUpdateCommand.oldPassword() == null
			&& userUpdateCommand.newPassword() == null) {
			throw new UserException(UserErrorCode.NOTHING_TO_UPDATE);
		}

		if (userUpdateCommand.nickname() != null) {
			validNickname(userUpdateCommand.nickname());

			user.updateNickname(userUpdateCommand.nickname());
		}

		if (userUpdateCommand.oldPassword() != null && userUpdateCommand.newPassword() != null) {
			validPassword(userUpdateCommand.oldPassword(), user.getPassword());

			validNewPassword(userUpdateCommand.newPassword(), user.getPassword());

			user.updatePassword(passwordEncoder.encode(userUpdateCommand.newPassword()));
		}

		return userRepository.save(user);
	}

	/**
	 * 전화번호 검증
	 * @param phone 전화번호
	 * @throws UserException 중복된 전화번호 존재할 시 예외 발생
	 * @author 이승현
	 */
	public void validPhone(String phone) {
		phone = phone.replaceAll("-", "");

		if (userRepository.existsByPhone(phone)) {
			throw new UserException(UserErrorCode.ALREADY_EXISTS_PHONE);
		}
	}

	/**
	 * 유저 포인트 충전
	 * @param command 포인트충전DTO
	 * @author 정석현
	 */
	public void chargeUserPoint(UserPointChargeCommand command) {
		User user = findActiveUserById(command.userId());
		user.addPoint(command.point());
	}

	/**
	 * 이메일과 유저 타입으로 검사
	 * @param email 이메일
	 * @param oAuthType 유저 타입 (DOMAIN, KAKAO, NAVER)
	 * @return boolean
	 * @author 이승현
	 */
	public boolean validEmailWithOAuthType(String email, OAuthType oAuthType) {
		return userRepository.existsByEmailAndOAuthType(email, oAuthType);
	}

}
