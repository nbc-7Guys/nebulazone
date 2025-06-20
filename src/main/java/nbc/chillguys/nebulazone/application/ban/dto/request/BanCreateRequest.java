package nbc.chillguys.nebulazone.application.ban.dto.request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BanCreateRequest(
	@NotBlank(message = "IP 주소는 필수입니다.")
	@Pattern(
		regexp = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$",
		message = "IP 형식이 올바르지 않습니다."
	)
	String ipAddress,

	@NotBlank(message = "공격 유형은 필수입니다.")
	@Size(max = 50, message = "공격 유형은 50자 이하로 입력해주세요.")
	String attackType,
	String reason,
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	LocalDateTime expiresAt
) {
}
