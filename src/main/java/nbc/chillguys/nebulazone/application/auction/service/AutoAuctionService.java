package nbc.chillguys.nebulazone.application.auction.service;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.domain.auction.entity.Auction;
import nbc.chillguys.nebulazone.domain.auction.service.AuctionDomainService;
import nbc.chillguys.nebulazone.domain.auction.service.AutoAuctionDomainService;
import nbc.chillguys.nebulazone.domain.bid.entity.Bid;
import nbc.chillguys.nebulazone.domain.bid.service.BidDomainService;
import nbc.chillguys.nebulazone.domain.product.entity.Product;
import nbc.chillguys.nebulazone.domain.product.service.ProductDomainService;
import nbc.chillguys.nebulazone.domain.transaction.dto.TransactionCreateCommand;
import nbc.chillguys.nebulazone.domain.transaction.entity.UserType;
import nbc.chillguys.nebulazone.domain.transaction.service.TransactionDomainService;

@Service
@RequiredArgsConstructor
public class AutoAuctionService {

	private final AuctionDomainService auctionDomainService;
	private final TransactionDomainService txDomainService;
	private final ProductDomainService productDomainService;
	private final AutoAuctionDomainService autoAuctionDomainService;
	private final BidDomainService bidDomainService;

	/**
	 * 자동 낙찰 시 자동으로 거래내역을 생성하는 로직
	 * @param auctionId 종료된 경매 id
	 * @param productId 경매 상품 id
	 * @author 전나겸
	 */
	@Async
	@Transactional
	public void autoEndAuctionAndCreateTransaction(Long auctionId, Long productId) {

		Auction auction = auctionDomainService.findActiveAuctionById(auctionId);
		Product product = productDomainService.findActiveProductById(productId);
		product.purchase();

		productDomainService.saveProductToEs(product);

		Bid wonBid = bidDomainService.findHighBidByAuction(auction.getId());
		autoAuctionDomainService.endAutoAuction(auctionId, wonBid);

		if (wonBid == null) {
			return;
		}

		List<Bid> bidList = bidDomainService.findBidsByAuctionIdAndStatusBid(auctionId);
		bidList.forEach(bid -> bid.getUser().addPoint(bid.getPrice()));

		product.getSeller().addPoint(wonBid.getPrice());

		TransactionCreateCommand buyerTxCreateCommand =
			TransactionCreateCommand.of(wonBid.getUser(), UserType.BUYER,
				product, product.getTxMethod().name(), wonBid.getPrice());

		txDomainService.createTransaction(buyerTxCreateCommand);

		TransactionCreateCommand sellerTxCreateCommand =
			TransactionCreateCommand.of(product.getSeller(), UserType.SELLER, product, product.getTxMethod().name(),
				wonBid.getPrice());

		txDomainService.createTransaction(sellerTxCreateCommand);
	}
}
