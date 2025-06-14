package nbc.chillguys.nebulazone.application.transaction.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.application.transaction.dto.response.FindDetailTransactionResponse;
import nbc.chillguys.nebulazone.application.transaction.dto.response.FindTransactionResponse;
import nbc.chillguys.nebulazone.application.transaction.service.TransactionService;
import nbc.chillguys.nebulazone.common.response.CommonPageResponse;
import nbc.chillguys.nebulazone.domain.auth.vo.AuthUser;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transactions")
public class TransactionController {

	private final TransactionService txService;

	@GetMapping("/me")
	public ResponseEntity<CommonPageResponse<FindTransactionResponse>> findMyTransactions(
		@AuthenticationPrincipal AuthUser authUser,
		@RequestParam(defaultValue = "1", value = "page") int page,
		@RequestParam(defaultValue = "20", value = "size") int size) {

		int zeroBasedPage = Math.max(page - 1, 0);

		CommonPageResponse<FindTransactionResponse> response =
			txService.findMyTransactions(authUser, zeroBasedPage, size);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{transactionId}/me")
	public ResponseEntity<FindDetailTransactionResponse> findMyTransaction(
		@AuthenticationPrincipal AuthUser authUser,
		@PathVariable("transactionId") Long transactionId) {

		FindDetailTransactionResponse response = txService.findMyTransaction(authUser, transactionId);
		return ResponseEntity.ok(response);
	}
}
