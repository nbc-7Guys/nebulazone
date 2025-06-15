package nbc.chillguys.nebulazone.application.bid.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import nbc.chillguys.nebulazone.application.bid.dto.request.CreateBidRequest;
import nbc.chillguys.nebulazone.application.bid.dto.response.CreateBidResponse;
import nbc.chillguys.nebulazone.application.bid.dto.response.DeleteBidResponse;
import nbc.chillguys.nebulazone.application.bid.dto.response.FindBidResponse;
import nbc.chillguys.nebulazone.common.response.CommonPageResponse;
import nbc.chillguys.nebulazone.domain.auction.entity.Auction;
import nbc.chillguys.nebulazone.domain.auction.exception.AuctionErrorCode;
import nbc.chillguys.nebulazone.domain.auction.exception.AuctionException;
import nbc.chillguys.nebulazone.domain.auction.service.AuctionDomainService;
import nbc.chillguys.nebulazone.domain.auth.vo.AuthUser;
import nbc.chillguys.nebulazone.domain.bid.dto.FindBidInfo;
import nbc.chillguys.nebulazone.domain.bid.entity.Bid;
import nbc.chillguys.nebulazone.domain.bid.entity.BidStatus;
import nbc.chillguys.nebulazone.domain.bid.exception.BidErrorCode;
import nbc.chillguys.nebulazone.domain.bid.exception.BidException;
import nbc.chillguys.nebulazone.domain.bid.service.BidDomainService;
import nbc.chillguys.nebulazone.domain.catalog.entity.Catalog;
import nbc.chillguys.nebulazone.domain.catalog.entity.CatalogType;
import nbc.chillguys.nebulazone.domain.products.entity.Product;
import nbc.chillguys.nebulazone.domain.products.entity.ProductTxMethod;
import nbc.chillguys.nebulazone.domain.user.entity.OAuthType;
import nbc.chillguys.nebulazone.domain.user.entity.User;
import nbc.chillguys.nebulazone.domain.user.entity.UserRole;
import nbc.chillguys.nebulazone.domain.user.exception.UserErrorCode;
import nbc.chillguys.nebulazone.domain.user.exception.UserException;
import nbc.chillguys.nebulazone.domain.user.service.UserDomainService;

@DisplayName("입찰 서비스 단위 테스트")
@ExtendWith({MockitoExtension.class})
class BidServiceUnitTest {

	@Mock
	BidDomainService bidDomainService;

	@Mock
	UserDomainService userDomainService;

	@Mock
	AuctionDomainService auctionDomainService;

	@InjectMocks
	BidService bidService;

	private static final String BIDDER_EMAIL = "bidder@test.com";
	private static final String BIDDER_NICKNAME = "입찰자";
	private static final String SELLER_EMAIL = "seller@test.com";
	private static final String SELLER_NICKNAME = "판매자";
	private static final String PRODUCT_NAME = "테스트 상품";
	private static final Long BID_PRICE = 150000L;
	private static final Long START_PRICE = 100000L;

	private User bidder;
	private User seller;
	private Catalog catalog;
	private Product product;
	private Auction auction;
	private AuthUser authUser;
	private CreateBidRequest createBidRequest;

	@BeforeEach
	void setUp() {
		bidder = createUser(1L, BIDDER_EMAIL, BIDDER_NICKNAME);
		seller = createUser(2L, SELLER_EMAIL, SELLER_NICKNAME);
		catalog = createCatalog(1L, "테스트 카탈로그");
		product = createProduct(1L, PRODUCT_NAME, seller, catalog);
		auction = createAuction(1L, product, START_PRICE);
		authUser = createAuthUser(1L, BIDDER_EMAIL);
		createBidRequest = new CreateBidRequest(BID_PRICE);
	}

	@Nested
	@DisplayName("입찰 생성")
	class CreateBidTest {

		@Test
		@DisplayName("입찰 생성 성공")
		void success_createBid() {
			// given
			Long auctionId = 1L;
			Bid createdBid = createBid(100L, bidder, BID_PRICE);

			given(auctionDomainService.findActiveAuctionWithProductAndSellerLock(auctionId)).willReturn(auction);
			given(userDomainService.findActiveUserById(authUser.getId())).willReturn(bidder);
			given(bidDomainService.createBid(auction, bidder, BID_PRICE)).willReturn(createdBid);

			// when
			CreateBidResponse result = bidService.createBid(auctionId, authUser, createBidRequest);

			// then
			assertThat(result.bidId()).isEqualTo(100L);
			assertThat(result.bidPrice()).isEqualTo(BID_PRICE);

			verify(auctionDomainService).findActiveAuctionWithProductAndSellerLock(auctionId);
			verify(userDomainService).findActiveUserById(authUser.getId());
			verify(bidDomainService).createBid(auction, bidder, BID_PRICE);
		}

		@Test
		@DisplayName("입찰 생성 실패 - 경매를 찾을 수 없음")
		void fail_createBid_auctionNotFound() {
			// given
			Long auctionId = 999L;

			given(auctionDomainService.findActiveAuctionWithProductAndSellerLock(auctionId))
				.willThrow(new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> bidService.createBid(auctionId, authUser, createBidRequest))
				.isInstanceOf(AuctionException.class)
				.extracting("errorCode")
				.isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);

			verify(auctionDomainService).findActiveAuctionWithProductAndSellerLock(auctionId);
			verify(userDomainService, never()).findActiveUserById(any());
			verify(bidDomainService, never()).createBid(any(), any(), any());
		}

		@Test
		@DisplayName("입찰 생성 실패 - 사용자를 찾을 수 없음")
		void fail_createBid_userNotFound() {
			// given
			Long auctionId = 1L;

			given(auctionDomainService.findActiveAuctionWithProductAndSellerLock(auctionId)).willReturn(auction);
			given(userDomainService.findActiveUserById(authUser.getId()))
				.willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> bidService.createBid(auctionId, authUser, createBidRequest))
				.isInstanceOf(UserException.class)
				.extracting("errorCode")
				.isEqualTo(UserErrorCode.USER_NOT_FOUND);

			verify(auctionDomainService).findActiveAuctionWithProductAndSellerLock(auctionId);
			verify(userDomainService).findActiveUserById(authUser.getId());
			verify(bidDomainService, never()).createBid(any(), any(), any());
		}

		@Test
		@DisplayName("입찰 생성 실패 - 현재 입찰가보다 낮은 가격으로 입찰")
		void fail_createBid_lowBidPrice() {
			// given
			Long auctionId = 1L;

			given(auctionDomainService.findActiveAuctionWithProductAndSellerLock(auctionId)).willReturn(auction);
			given(userDomainService.findActiveUserById(authUser.getId())).willReturn(bidder);
			given(bidDomainService.createBid(auction, bidder, BID_PRICE))
				.willThrow(new BidException(BidErrorCode.BID_PRICE_TOO_LOW));

			// when & then
			assertThatThrownBy(() -> bidService.createBid(auctionId, authUser, createBidRequest))
				.isInstanceOf(BidException.class)
				.extracting("errorCode")
				.isEqualTo(BidErrorCode.BID_PRICE_TOO_LOW);

			verify(auctionDomainService).findActiveAuctionWithProductAndSellerLock(auctionId);
			verify(userDomainService).findActiveUserById(authUser.getId());
			verify(bidDomainService).createBid(auction, bidder, BID_PRICE);
		}
	}

	@Nested
	@DisplayName("입찰 조회")
	class FindBidsTest {

		@Test
		@DisplayName("경매별 입찰 목록 조회 성공")
		void success_findBids() {
			// given
			Long auctionId = 1L;
			int page = 0;
			int size = 10;
			List<FindBidInfo> bidInfoList = createFindBidInfoList();
			PageRequest pageRequest = PageRequest.of(page, size);
			Page<FindBidInfo> pageResult = new PageImpl<>(bidInfoList, pageRequest, bidInfoList.size());

			given(auctionDomainService.findActiveAuctionById(auctionId)).willReturn(auction);
			given(bidDomainService.findBids(auction, page, size)).willReturn(pageResult);

			// when
			CommonPageResponse<FindBidResponse> result = bidService.findBids(auctionId, page, size);

			// then
			assertThat(result.content()).hasSize(2);
			assertThat(result.content().get(0).BidId()).isEqualTo(100L);
			assertThat(result.content().get(0).nickname()).isEqualTo(BIDDER_NICKNAME);
			assertThat(result.content().get(0).bidPrice()).isEqualTo(BID_PRICE);
			assertThat(result.content().get(0).productName()).isEqualTo(PRODUCT_NAME);
			assertThat(result.totalElements()).isEqualTo(2L);

			verify(auctionDomainService).findActiveAuctionById(auctionId);
			verify(bidDomainService).findBids(auction, page, size);
		}

		@Test
		@DisplayName("내 입찰 목록 조회 성공")
		void success_findMyBids() {
			// given
			int page = 0;
			int size = 10;
			List<FindBidInfo> bidInfoList = createFindBidInfoList();
			PageRequest pageRequest = PageRequest.of(page, size);
			Page<FindBidInfo> pageResult = new PageImpl<>(bidInfoList, pageRequest, bidInfoList.size());

			given(userDomainService.findActiveUserById(authUser.getId())).willReturn(bidder);
			given(bidDomainService.findMyBids(bidder, page, size)).willReturn(pageResult);

			// when
			CommonPageResponse<FindBidResponse> result = bidService.findMyBids(authUser, page, size);

			// then
			assertThat(result.content()).hasSize(2);
			assertThat(result.content().get(0).BidId()).isEqualTo(100L);
			assertThat(result.content().get(0).nickname()).isEqualTo(BIDDER_NICKNAME);
			assertThat(result.content().get(0).bidPrice()).isEqualTo(BID_PRICE);
			assertThat(result.totalElements()).isEqualTo(2L);

			verify(userDomainService).findActiveUserById(authUser.getId());
			verify(bidDomainService).findMyBids(bidder, page, size);
		}

		@Test
		@DisplayName("경매별 입찰 목록 조회 실패 - 경매를 찾을 수 없음")
		void fail_findBids_auctionNotFound() {
			// given
			Long auctionId = 999L;
			int page = 0;
			int size = 10;

			given(auctionDomainService.findActiveAuctionById(auctionId))
				.willThrow(new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> bidService.findBids(auctionId, page, size))
				.isInstanceOf(AuctionException.class)
				.extracting("errorCode")
				.isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);

			verify(auctionDomainService).findActiveAuctionById(auctionId);
			verify(bidDomainService, never()).findBids(any(), anyInt(), anyInt());
		}

		@Test
		@DisplayName("내 입찰 목록 조회 실패 - 사용자를 찾을 수 없음")
		void fail_findMyBids_userNotFound() {
			// given
			int page = 0;
			int size = 10;

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> bidService.findMyBids(authUser, page, size))
				.isInstanceOf(UserException.class)
				.extracting("errorCode")
				.isEqualTo(UserErrorCode.USER_NOT_FOUND);

			verify(userDomainService).findActiveUserById(authUser.getId());
			verify(bidDomainService, never()).findMyBids(any(), anyInt(), anyInt());
		}
	}

	@Nested
	@DisplayName("입찰 취소 테스트(상태 변경)")
	class StatusBidTest {

		@Test
		@DisplayName("입찰 취소 성공")
		void success_statusBid() {
			// given
			Long auctionId = 1L;
			Long bidId = 100L;

			given(userDomainService.findActiveUserById(authUser.getId())).willReturn(bidder);
			given(auctionDomainService.findActiveAuctionWithProductAndSellerLock(auctionId)).willReturn(auction);
			given(bidDomainService.statusBid(auction, bidder, bidId)).willReturn(bidId);

			// when
			DeleteBidResponse result = bidService.statusBid(authUser, auctionId, bidId);

			// then
			assertThat(result.commentId()).isEqualTo(bidId);

			verify(userDomainService).findActiveUserById(authUser.getId());
			verify(auctionDomainService).findActiveAuctionWithProductAndSellerLock(auctionId);
			verify(bidDomainService).statusBid(auction, bidder, bidId);
		}

		@Test
		@DisplayName("입찰 취소 실패 - 사용자를 찾을 수 없음")
		void fail_statusBid_userNotFound() {
			// given
			Long auctionId = 1L;
			Long bidId = 100L;

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> bidService.statusBid(authUser, auctionId, bidId))
				.isInstanceOf(UserException.class)
				.extracting("errorCode")
				.isEqualTo(UserErrorCode.USER_NOT_FOUND);

			verify(userDomainService).findActiveUserById(authUser.getId());
			verify(auctionDomainService, never()).findActiveAuctionWithProductAndSellerLock(any());
			verify(bidDomainService, never()).statusBid(any(), any(), any());
		}

		@Test
		@DisplayName("입찰 취소 실패 - 경매를 찾을 수 없음")
		void fail_statusBid_auctionNotFound() {
			// given
			Long auctionId = 999L;
			Long bidId = 100L;

			given(userDomainService.findActiveUserById(authUser.getId())).willReturn(bidder);
			given(auctionDomainService.findActiveAuctionWithProductAndSellerLock(auctionId))
				.willThrow(new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> bidService.statusBid(authUser, auctionId, bidId))
				.isInstanceOf(AuctionException.class)
				.extracting("errorCode")
				.isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);

			verify(userDomainService).findActiveUserById(authUser.getId());
			verify(auctionDomainService).findActiveAuctionWithProductAndSellerLock(auctionId);
			verify(bidDomainService, never()).statusBid(any(), any(), any());
		}

		@Test
		@DisplayName("입찰 취소 실패 - 입찰을 찾을 수 없음")
		void fail_statusBid_bidNotFound() {
			// given
			Long auctionId = 1L;
			Long bidId = 999L;

			given(userDomainService.findActiveUserById(authUser.getId())).willReturn(bidder);
			given(auctionDomainService.findActiveAuctionWithProductAndSellerLock(auctionId)).willReturn(auction);
			given(bidDomainService.statusBid(auction, bidder, bidId))
				.willThrow(new BidException(BidErrorCode.BID_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> bidService.statusBid(authUser, auctionId, bidId))
				.isInstanceOf(BidException.class)
				.extracting("errorCode")
				.isEqualTo(BidErrorCode.BID_NOT_FOUND);

			verify(userDomainService).findActiveUserById(authUser.getId());
			verify(auctionDomainService).findActiveAuctionWithProductAndSellerLock(auctionId);
			verify(bidDomainService).statusBid(auction, bidder, bidId);
		}

		@Test
		@DisplayName("입찰 취소 실패 - 권한 없음")
		void fail_statusBid_unauthorized() {
			// given
			Long auctionId = 1L;
			Long bidId = 100L;

			given(userDomainService.findActiveUserById(authUser.getId())).willReturn(bidder);
			given(auctionDomainService.findActiveAuctionWithProductAndSellerLock(auctionId)).willReturn(auction);
			given(bidDomainService.statusBid(auction, bidder, bidId))
				.willThrow(new BidException(BidErrorCode.BID_NOT_OWNER));

			// when & then
			assertThatThrownBy(() -> bidService.statusBid(authUser, auctionId, bidId))
				.isInstanceOf(BidException.class)
				.extracting("errorCode")
				.isEqualTo(BidErrorCode.BID_NOT_OWNER);

			verify(userDomainService).findActiveUserById(authUser.getId());
			verify(auctionDomainService).findActiveAuctionWithProductAndSellerLock(auctionId);
			verify(bidDomainService).statusBid(auction, bidder, bidId);
		}
	}

	private User createUser(Long id, String email, String nickname) {
		User user = User.builder()
			.email(email)
			.nickname(nickname)
			.oAuthType(OAuthType.DOMAIN)
			.roles(Set.of(UserRole.ROLE_USER))
			.build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Catalog createCatalog(Long id, String name) {
		Catalog catalog = Catalog.builder()
			.name(name)
			.description("카탈로그 설명")
			.type(CatalogType.CPU)
			.build();
		ReflectionTestUtils.setField(catalog, "id", id);
		return catalog;
	}

	private Product createProduct(Long id, String name, User seller, Catalog catalog) {
		Product product = Product.builder()
			.name(name)
			.description("상품 설명")
			.price(START_PRICE)
			.txMethod(ProductTxMethod.AUCTION)
			.seller(seller)
			.catalog(catalog)
			.build();
		ReflectionTestUtils.setField(product, "id", id);
		return product;
	}

	private Auction createAuction(Long id, Product product, Long startPrice) {
		Auction auction = Auction.builder()
			.product(product)
			.startPrice(startPrice)
			.currentPrice(startPrice)
			.endTime(LocalDateTime.now().plusDays(7))
			.build();
		ReflectionTestUtils.setField(auction, "id", id);
		return auction;
	}

	private AuthUser createAuthUser(Long id, String email) {
		return AuthUser.builder()
			.id(id)
			.email(email)
			.roles(Set.of(UserRole.ROLE_USER))
			.build();
	}

	private Bid createBid(Long id, User user, Long price) {
		Bid bid = Bid.builder()
			.user(user)
			.price(price)
			.build();
		ReflectionTestUtils.setField(bid, "id", id);
		return bid;
	}

	private List<FindBidInfo> createFindBidInfoList() {
		FindBidInfo info1 = new FindBidInfo(
			100L,
			BID_PRICE,
			LocalDateTime.now(),
			BidStatus.BID,
			BIDDER_NICKNAME,
			PRODUCT_NAME
		);

		FindBidInfo info2 = new FindBidInfo(
			101L,
			120000L,
			LocalDateTime.now().minusMinutes(10),
			BidStatus.BID,
			"다른입찰자",
			PRODUCT_NAME
		);

		return List.of(info1, info2);
	}
}
