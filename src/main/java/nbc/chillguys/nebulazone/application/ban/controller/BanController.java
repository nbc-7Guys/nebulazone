package nbc.chillguys.nebulazone.application.ban.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.application.ban.dto.request.BanCreateRequest;
import nbc.chillguys.nebulazone.application.ban.sevice.BanService;

@RestController
@RequestMapping("/internal/bans")
@RequiredArgsConstructor
public class BanController {
	private final BanService banService;

	@PostMapping
	public ResponseEntity<Void> create(@RequestBody @Valid BanCreateRequest request) {
		banService.createBan(request);
		return ResponseEntity.ok().build();
	}
}
