package nbc.chillguys.nebulazone.application.auction.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.application.auction.dto.request.ManualEndAuctionRequest;
import nbc.chillguys.nebulazone.application.auction.dto.response.DeleteAuctionResponse;
import nbc.chillguys.nebulazone.application.auction.dto.response.FindAllAuctionResponse;
import nbc.chillguys.nebulazone.application.auction.dto.response.FindDetailAuctionResponse;
import nbc.chillguys.nebulazone.application.auction.dto.response.ManualEndAuctionResponse;
import nbc.chillguys.nebulazone.common.response.CommonPageResponse;
import nbc.chillguys.nebulazone.domain.auction.dto.AuctionFindAllInfo;
import nbc.chillguys.nebulazone.domain.auction.dto.AuctionFindDetailInfo;
import nbc.chillguys.nebulazone.domain.auction.dto.ManualEndAuctionInfo;
import nbc.chillguys.nebulazone.domain.auction.entity.AuctionSortType;
import nbc.chillguys.nebulazone.domain.auction.service.AuctionDomainService;
import nbc.chillguys.nebulazone.domain.bid.entity.Bid;
import nbc.chillguys.nebulazone.domain.bid.service.BidDomainService;
import nbc.chillguys.nebulazone.domain.product.entity.Product;
import nbc.chillguys.nebulazone.domain.product.service.ProductDomainService;
import nbc.chillguys.nebulazone.domain.transaction.dto.TransactionCreateCommand;
import nbc.chillguys.nebulazone.domain.transaction.entity.UserType;
import nbc.chillguys.nebulazone.domain.transaction.service.TransactionDomainService;
import nbc.chillguys.nebulazone.domain.user.entity.User;

@Service
@RequiredArgsConstructor
public class AuctionService {

	private final AuctionDomainService auctionDomainService;
	private final AuctionSchedulerService auctionSchedulerService;
	private final BidDomainService bidDomainService;
	private final TransactionDomainService txDomainService;
	private final ProductDomainService productDomainService;

	public CommonPageResponse<FindAllAuctionResponse> findAuctions(int page, int size) {

		Page<AuctionFindAllInfo> findAuctions = auctionDomainService.findAuctions(page, size);
		Page<FindAllAuctionResponse> response = findAuctions.map(FindAllAuctionResponse::from);

		return CommonPageResponse.from(response);
	}

	public List<FindAllAuctionResponse> findAuctionsBySortType(AuctionSortType sortType) {

		List<AuctionFindAllInfo> findAuctionsBySortType = auctionDomainService.findAuctionsBySortType(sortType);

		return findAuctionsBySortType.stream().map(FindAllAuctionResponse::from).toList();
	}

	@Transactional
	public DeleteAuctionResponse deleteAuction(Long auctionId, User user) {

		Long deletedAuctionId = auctionDomainService.deleteAuction(auctionId, user);
		auctionSchedulerService.cancelSchedule(deletedAuctionId);

		return DeleteAuctionResponse.from(deletedAuctionId);
	}

	@Transactional
	public ManualEndAuctionResponse manualEndAuction(Long auctionId, User user,
		ManualEndAuctionRequest request) {

		Bid wonBid = bidDomainService.findBid(request.bidId());
		Product product = productDomainService.findActiveProductById(request.productId());
		product.purchase();

		productDomainService.saveProductToEs(product);

		ManualEndAuctionInfo auctionInfo = auctionDomainService.manualEndAuction(user, wonBid, auctionId);

		List<Bid> bidList = bidDomainService.findBidsByAuctionIdAndStatusBid(auctionId);
		bidList.forEach(bid -> bid.getUser().addPoint(bid.getPrice()));

		product.getSeller().addPoint(wonBid.getPrice());

		TransactionCreateCommand buyerTxCreateCommand = TransactionCreateCommand.of(wonBid.getUser(), UserType.BUYER,
			product, product.getTxMethod().name(), auctionInfo.wonProductPrice());

		txDomainService.createTransaction(buyerTxCreateCommand);

		TransactionCreateCommand sellerTxCreateCommand = TransactionCreateCommand.of(product.getSeller(),
			UserType.SELLER, product, product.getTxMethod().name(), auctionInfo.wonProductPrice());

		txDomainService.createTransaction(sellerTxCreateCommand);

		return ManualEndAuctionResponse.from(auctionInfo);
	}

	public FindDetailAuctionResponse findAuction(Long auctionId) {

		Bid highestPriceBid = bidDomainService.findHighBidByAuction(auctionId);
		AuctionFindDetailInfo auctionFindDetailInfo = auctionDomainService.findAuction(auctionId);

		return FindDetailAuctionResponse.from(auctionFindDetailInfo, highestPriceBid);
	}

}
