package nbc.chillguys.nebulazone.application.auction.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.domain.auction.entity.Auction;
import nbc.chillguys.nebulazone.domain.auction.service.AuctionDomainService;
import nbc.chillguys.nebulazone.domain.auction.service.AutoAuctionDomainService;
import nbc.chillguys.nebulazone.domain.bid.entity.Bid;
import nbc.chillguys.nebulazone.domain.bid.service.BidDomainService;
import nbc.chillguys.nebulazone.domain.products.entity.Product;
import nbc.chillguys.nebulazone.domain.products.service.ProductDomainService;
import nbc.chillguys.nebulazone.domain.transaction.dto.TransactionCreateCommand;
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

		Bid wonBid = bidDomainService.findHighBidByAuction(auction.getId());
		autoAuctionDomainService.endAutoAuction(auctionId, wonBid);

		if (wonBid == null) {
			return;
		}

		TransactionCreateCommand txCreateCommand =
			TransactionCreateCommand.of(wonBid.getUser(), product, product.getTxMethod().name(), wonBid.getPrice());

		txDomainService.createTransaction(txCreateCommand);
	}
}
