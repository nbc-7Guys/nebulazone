package nbc.chillguys.nebulazone.domain.bid.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.domain.auction.entity.Auction;
import nbc.chillguys.nebulazone.domain.auction.exception.AuctionErrorCode;
import nbc.chillguys.nebulazone.domain.auction.exception.AuctionException;
import nbc.chillguys.nebulazone.domain.bid.dto.FindBidInfo;
import nbc.chillguys.nebulazone.domain.bid.entity.Bid;
import nbc.chillguys.nebulazone.domain.bid.entity.BidStatus;
import nbc.chillguys.nebulazone.domain.bid.exception.BidErrorCode;
import nbc.chillguys.nebulazone.domain.bid.exception.BidException;
import nbc.chillguys.nebulazone.domain.bid.repository.BidRepository;
import nbc.chillguys.nebulazone.domain.user.entity.User;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidDomainService {

	private final BidRepository bidRepository;

	/**
	 * 입찰 생성
	 * @param lockAuction 삭제되지 않은 비관적 락이 적용된 Auction(상품, 셀러 정보 포함)
	 * @param user 입찰자
	 * @param price 입찰 가격
	 * @return Bid
	 * @author 전나겸
	 */
	@Transactional
	public Bid createBid(Auction lockAuction, User user, Long price) {

		if (Duration.between(LocalDateTime.now(), lockAuction.getEndTime()).isNegative()) {
			throw new AuctionException(AuctionErrorCode.ALREADY_CLOSED_AUCTION);
		}

		if (lockAuction.isWon()) {
			throw new AuctionException(AuctionErrorCode.ALREADY_WON_AUCTION);
		}

		if (lockAuction.isAuctionOwner(user)) {
			throw new BidException(BidErrorCode.CANNOT_BID_OWN_AUCTION);
		}

		Optional<Long> highestPrice = bidRepository.findActiveBidHighestPriceByAuction(lockAuction);

		if (highestPrice.isPresent() && highestPrice.get() >= price) {
			throw new BidException(BidErrorCode.BID_PRICE_TOO_LOW);
		}

		lockAuction.updateBidPrice(price);
		Bid bid = Bid.builder()
			.auction(lockAuction)
			.user(user)
			.price(price)
			.build();

		return bidRepository.save(bid);
	}

	/**
	 * 특정 경매의 입찰 내역 조회
	 * @param auction 조회할 삭제되지 않은 경매
	 * @param page 페이지
	 * @param size 출력 개수
	 * @return FindBidInfo 페이징
	 * @author 전나겸
	 */
	public Page<FindBidInfo> findBids(Auction auction, int page, int size) {

		return bidRepository.findBidsWithUserByAuction(auction, page, size);
	}

	/**
	 * 내 입찰 내역 조회
	 * @param user 로그인 유저
	 * @param page 페이지
	 * @param size 출력 개수
	 * @return FindBidInfo 페이징
	 * @author 전나겸
	 */
	public Page<FindBidInfo> findMyBids(User user, int page, int size) {

		return bidRepository.findMyBids(user, page, size);
	}

	/**
	 * 내 입찰 취소
	 * @param lockAuction 삭제되지 않은 경매(락 적용)
	 * @param user 로그인한 유저
	 * @param bidId 취소할 입찰 Id
	 * @return 취소한 입찰 Id
	 * @author 전나겸
	 */
	@Transactional
	public Long statusBid(Auction lockAuction, User user, Long bidId) {

		if (Duration.between(LocalDateTime.now(), lockAuction.getEndTime()).isNegative()) {
			throw new AuctionException(AuctionErrorCode.ALREADY_CLOSED_AUCTION);
		}

		if (Duration.between(LocalDateTime.now(), lockAuction.getEndTime()).toMinutes() < 30) {
			throw new BidException(BidErrorCode.BID_CANCEL_TIME_LIMIT_EXCEEDED);
		}

		if (lockAuction.isWon()) {
			throw new AuctionException(AuctionErrorCode.ALREADY_WON_AUCTION);
		}

		Bid findBid = bidRepository.findById(bidId)
			.orElseThrow(() -> new BidException(BidErrorCode.BID_NOT_FOUND));

		if (findBid.getStatus() == BidStatus.WON) {
			throw new BidException(BidErrorCode.CANNOT_CANCEL_WON_BID);
		}

		if (findBid.getStatus() == BidStatus.CANCEL) {
			throw new BidException(BidErrorCode.ALREADY_BID_CANCELLED);
		}

		if (findBid.isNotBidOwner(user)) {
			throw new BidException(BidErrorCode.BID_NOT_OWNER);
		}

		if (findBid.isDifferentAuction(lockAuction)) {
			throw new BidException(BidErrorCode.BID_AUCTION_MISMATCH);
		}

		findBid.cancelBid();

		Long beforeHighestPrice = bidRepository.findActiveBidHighestPriceByAuction(lockAuction)
			.orElse(null);
		lockAuction.updateBidPrice(beforeHighestPrice);

		return findBid.getId();
	}

	/**
	 * 특정 경매의 최고가 입찰 조회<br>
	 * 유저를 함께 조회
	 * @param auctionId 경매 id
	 * @return 조회된 Bid
	 * @author 전나겸
	 */
	public Bid findHighBidByAuction(Long auctionId) {
		return bidRepository.findHighestPriceBidByAuctionWithUser(auctionId);
	}

	/**
	 * 특정 입찰 조회(유저도 함께 조회)
	 * @param bidId 조회할 입찰 Id
	 * @return 조회된 Bid
	 * @author 전나겸
	 */
	public Bid findBid(Long bidId) {
		return bidRepository.findBidWithWonUser(bidId)
			.orElseThrow(() -> new BidException(BidErrorCode.BID_NOT_FOUND));
	}

}
