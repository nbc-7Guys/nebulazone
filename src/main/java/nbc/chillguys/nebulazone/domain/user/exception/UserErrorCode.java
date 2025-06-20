package nbc.chillguys.nebulazone.domain.user.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
	NOTHING_TO_UPDATE(HttpStatus.BAD_REQUEST, "수정할 내용이 없습니다."),
	WRONG_ROLES(HttpStatus.BAD_REQUEST, "유저 권한을 잘못 입력하였습니다."),
	SAME_PASSWORD(HttpStatus.BAD_REQUEST, "동일한 비밀번호로 변경할 수 없습니다."),
	WRONG_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 틀렸습니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
	ALREADY_EXISTS_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일이 있습니다."),
	ALREADY_EXISTS_NICKNAME(HttpStatus.CONFLICT, "이미 존재하는 닉네임이 있습니다."),
	ALREADY_EXISTS_PHONE(HttpStatus.CONFLICT, "이미 존재하는 핸드폰번호가 있습니다."),
	UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 제공자입니다."),
	INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "포인트가 부족합니다.");

	private final HttpStatus status;
	private final String message;
}
