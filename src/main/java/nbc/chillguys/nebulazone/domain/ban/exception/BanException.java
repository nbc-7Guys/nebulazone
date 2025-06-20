package nbc.chillguys.nebulazone.domain.ban.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.common.exception.BaseException;

@Getter
@RequiredArgsConstructor
public class BanException extends BaseException {
	private final BanErrorCode errorCode;
}
