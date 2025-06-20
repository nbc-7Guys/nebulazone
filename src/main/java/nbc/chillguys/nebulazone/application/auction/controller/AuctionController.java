package nbc.chillguys.nebulazone.application.auction.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.application.auction.dto.request.ManualEndAuctionRequest;
import nbc.chillguys.nebulazone.application.auction.dto.response.DeleteAuctionResponse;
import nbc.chillguys.nebulazone.application.auction.dto.response.FindAllAuctionResponse;
import nbc.chillguys.nebulazone.application.auction.dto.response.FindDetailAuctionResponse;
import nbc.chillguys.nebulazone.application.auction.dto.response.ManualEndAuctionResponse;
import nbc.chillguys.nebulazone.application.auction.service.AuctionService;
import nbc.chillguys.nebulazone.common.response.CommonPageResponse;
import nbc.chillguys.nebulazone.domain.auction.entity.AuctionSortType;
import nbc.chillguys.nebulazone.domain.user.entity.User;

@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionController {

	private final AuctionService auctionService;

	@GetMapping
	public ResponseEntity<CommonPageResponse<FindAllAuctionResponse>> findAuctions(
		@RequestParam(defaultValue = "1", value = "page") int page,
		@RequestParam(defaultValue = "20", value = "size") int size) {

		CommonPageResponse<FindAllAuctionResponse> response = auctionService.findAuctions(Math.max(page - 1, 0), size);

		return ResponseEntity.ok(response);

	}

	@GetMapping("/sorted")
	public ResponseEntity<List<FindAllAuctionResponse>> findAuctionsSortType(
		@RequestParam("sort") String sortType) {

		List<FindAllAuctionResponse> response = auctionService.findAuctionsBySortType(AuctionSortType.of(sortType));

		return ResponseEntity.ok(response);

	}

	@GetMapping("/{auctionId}")
	public ResponseEntity<FindDetailAuctionResponse> findAuction(@PathVariable("auctionId") Long auctionId) {

		FindDetailAuctionResponse response = auctionService.findAuction(auctionId);

		return ResponseEntity.ok(response);

	}

	@PostMapping("/{auctionId}")
	public ResponseEntity<ManualEndAuctionResponse> manualEndAuction(
		@PathVariable("auctionId") Long auctionId,
		@AuthenticationPrincipal User user,
		@Valid @RequestBody ManualEndAuctionRequest request) {

		ManualEndAuctionResponse response = auctionService.manualEndAuction(auctionId, user, request);

		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{auctionId}")
	public ResponseEntity<DeleteAuctionResponse> deleteAuction(
		@PathVariable("auctionId") Long auctionId,
		@AuthenticationPrincipal User user) {

		DeleteAuctionResponse response = auctionService.deleteAuction(auctionId, user);

		return ResponseEntity.ok(response);
	}
}
