package nbc.chillguys.nebulazone.domain.bid.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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
	 * 특정 경매의 입찰 생성 - 최초 입찰
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

		if (lockAuction.isAuctionOwner(user)) {
			throw new BidException(BidErrorCode.CANNOT_BID_OWN_AUCTION);
		}

		if (lockAuction.getStartPrice() > price) {
			throw new BidException(BidErrorCode.BID_PRICE_TOO_LOW_START_PRICE);
		}

		if (lockAuction.isWon()) {
			throw new AuctionException(AuctionErrorCode.ALREADY_WON_AUCTION);
		}

		Optional<Long> highestPrice = bidRepository.findActiveBidHighestPriceByAuction(lockAuction);

		if (highestPrice.isPresent() && highestPrice.get() >= price) {
			throw new BidException(BidErrorCode.BID_PRICE_TOO_LOW_CURRENT_PRICE);
		}

		lockAuction.updateBidPrice(price);
		user.usePoint(price);

		Bid bid = Bid.builder()
			.auction(lockAuction)
			.user(user)
			.price(price)
			.build();

		return bidRepository.save(bid);
	}

	/**
	 * 특정 경매의 입찰 수정 - 기존 입찰이 존재할 때
	 * @param lockAuction 경매
	 * @param findBid 기존 입찰 내역
	 * @param user 로그인 유저
	 * @param price 입찰가
	 * @return bid
	 * @author 전나겸
	 */
	@Transactional
	public Bid updateBid(Auction lockAuction, Bid findBid, User user, Long price) {

		if (Duration.between(LocalDateTime.now(), lockAuction.getEndTime()).isNegative()) {
			throw new AuctionException(AuctionErrorCode.ALREADY_CLOSED_AUCTION);
		}

		if (findBid.isNotBidOwner(user)) {
			throw new BidException(BidErrorCode.BID_NOT_OWNER);
		}

		if (findBid.isDifferentAuction(lockAuction)) {
			throw new BidException(BidErrorCode.BID_AUCTION_MISMATCH);
		}

		if (lockAuction.isAuctionOwner(user)) {
			throw new BidException(BidErrorCode.CANNOT_BID_OWN_AUCTION);
		}

		if (lockAuction.isWon()) {
			throw new AuctionException(AuctionErrorCode.ALREADY_WON_AUCTION);
		}

		Optional<Long> highestPrice = bidRepository.findActiveBidHighestPriceByAuction(lockAuction);

		if (highestPrice.isPresent() && highestPrice.get() >= price) {
			throw new BidException(BidErrorCode.BID_PRICE_TOO_LOW_CURRENT_PRICE);
		}

		lockAuction.updateBidPrice(price);
		user.updatePoint(findBid.getPrice(), price);
		findBid.updateBidPrice(price);
		return findBid;
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

	/**
	 * 특정 경매의 유저가 입찰한 bid 조회
	 * @param auctionId 특정 경매 id
	 * @param userId 조회할 유저 id
	 * @return 조회된 Bid
	 * @author 전나겸
	 */
	public Optional<Bid> findBidByAuctionIdAndUserId(Long auctionId, Long userId) {
		return bidRepository.findBidByAuctionIdAndUserId(auctionId, userId);
	}

	/**
	 * 해당 경매의 입찰 전체조회
	 * @param auctionId 조회할 경매
	 * @return 조회된 BidList
	 * @author 전나겸
	 */
	public List<Bid> findBidsByAuctionIdAndStatusBid(Long auctionId) {
		return bidRepository.findBidsByAuctionIdAndStatusBid(auctionId);
	}
}

