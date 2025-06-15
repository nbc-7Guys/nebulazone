package nbc.chillguys.nebulazone.application.auction.service;

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
import nbc.chillguys.nebulazone.domain.auth.vo.AuthUser;
import nbc.chillguys.nebulazone.domain.bid.entity.Bid;
import nbc.chillguys.nebulazone.domain.bid.exception.BidErrorCode;
import nbc.chillguys.nebulazone.domain.bid.exception.BidException;
import nbc.chillguys.nebulazone.domain.bid.service.BidDomainService;
import nbc.chillguys.nebulazone.domain.catalog.entity.Catalog;
import nbc.chillguys.nebulazone.domain.catalog.entity.CatalogType;
import nbc.chillguys.nebulazone.domain.products.entity.Product;
import nbc.chillguys.nebulazone.domain.products.entity.ProductTxMethod;
import nbc.chillguys.nebulazone.domain.products.exception.ProductErrorCode;
import nbc.chillguys.nebulazone.domain.products.exception.ProductException;
import nbc.chillguys.nebulazone.domain.products.service.ProductDomainService;
import nbc.chillguys.nebulazone.domain.transaction.service.TransactionDomainService;
import nbc.chillguys.nebulazone.domain.user.entity.OAuthType;
import nbc.chillguys.nebulazone.domain.user.entity.User;
import nbc.chillguys.nebulazone.domain.user.entity.UserRole;
import nbc.chillguys.nebulazone.domain.user.exception.UserErrorCode;
import nbc.chillguys.nebulazone.domain.user.exception.UserException;
import nbc.chillguys.nebulazone.domain.user.service.UserDomainService;

@DisplayName("경매 서비스 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AuctionServiceUnitTest {

	@Mock
	AuctionDomainService auctionDomainService;

	@Mock
	BidDomainService bidDomainService;

	@Mock
	UserDomainService userDomainService;

	@Mock
	AuctionSchedulerService auctionSchedulerService;

	@Mock
	ProductDomainService productDomainService;

	@Mock
	TransactionDomainService txDomainService;

	@InjectMocks
	AuctionService auctionService;

	private static final String SELLER_EMAIL = "seller@test.com";
	private static final String SELLER_NICKNAME = "판매자";
	private static final String BIDDER_EMAIL = "bidder@test.com";
	private static final String BIDDER_NICKNAME = "입찰자";
	private static final String PRODUCT_NAME = "테스트 상품";
	private static final Long START_PRICE = 100000L;
	private static final Long CURRENT_PRICE = 150000L;

	// 공통 테스트 픽스처
	private User seller;
	private User bidder;
	private Catalog catalog;
	private Product product;
	private AuthUser authUser;

	@BeforeEach
	void setUp() {
		seller = createUser(1L, SELLER_EMAIL, SELLER_NICKNAME);
		bidder = createUser(2L, BIDDER_EMAIL, BIDDER_NICKNAME);
		catalog = createCatalog(1L, "테스트 카탈로그");
		product = createProduct(1L, PRODUCT_NAME, seller, catalog);
		authUser = createAuthUser(1L, SELLER_EMAIL);
	}

	@Nested
	@DisplayName("경매 목록 조회")
	class FindAuctionsTest {

		@Test
		@DisplayName("경매 목록 조회 성공")
		void success_findAuctions() {
			// given
			int page = 0;
			int size = 10;
			List<AuctionFindAllInfo> content = createAuctionFindAllInfoList();
			PageRequest pageRequest = PageRequest.of(page, size);
			Page<AuctionFindAllInfo> pageResult = new PageImpl<>(content, pageRequest, content.size());

			given(auctionDomainService.findAuctions(page, size)).willReturn(pageResult);

			// when
			CommonPageResponse<FindAllAuctionResponse> result = auctionService.findAuctions(page, size);

			// then
			assertThat(result.content()).hasSize(2);
			assertThat(result.content().get(0).productName()).isEqualTo(PRODUCT_NAME);
			assertThat(result.content().get(0).startPrice()).isEqualTo(START_PRICE);
			assertThat(result.content().get(1).bidCount()).isEqualTo(5L);
			assertThat(result.totalElements()).isEqualTo(2L);
		}
	}

	@Nested
	@DisplayName("정렬별 경매 목록 조회")
	class FindAuctionsBySortTypeTest {

		@Test
		@DisplayName("인기순 경매 목록 조회 성공")
		void success_findAuctionsBySortType_popular() {
			// given
			AuctionSortType sortType = AuctionSortType.POPULAR;
			List<AuctionFindAllInfo> auctionInfoList = createAuctionFindAllInfoList();

			given(auctionDomainService.findAuctionsBySortType(sortType)).willReturn(auctionInfoList);

			// when
			List<FindAllAuctionResponse> result = auctionService.findAuctionsBySortType(sortType);

			// then
			assertThat(result).hasSize(2);
			assertThat(result.get(0).productName()).isEqualTo(PRODUCT_NAME);
			assertThat(result.get(0).startPrice()).isEqualTo(START_PRICE);
			assertThat(result.get(1).currentPrice()).isEqualTo(125000L);
			assertThat(result).extracting("auctionId").containsExactly(1L, 2L);
		}

		@Test
		@DisplayName("마감 임박순 경매 목록 조회 성공")
		void success_findAuctionsBySortType_deadlineImminent() {
			// given
			AuctionSortType sortType = AuctionSortType.CLOSING;
			List<AuctionFindAllInfo> auctionInfoList = createAuctionFindAllInfoList();

			given(auctionDomainService.findAuctionsBySortType(sortType)).willReturn(auctionInfoList);

			// when
			List<FindAllAuctionResponse> result = auctionService.findAuctionsBySortType(sortType);

			// then
			assertThat(result).hasSize(2);
			assertThat(result.get(0).productName()).isEqualTo(PRODUCT_NAME);
			assertThat(result.get(0).startPrice()).isEqualTo(START_PRICE);
			assertThat(result.get(1).currentPrice()).isEqualTo(125000L);
			assertThat(result).extracting("auctionId").containsExactly(1L, 2L);
		}
	}

	@Nested
	@DisplayName("경매 삭제")
	class DeleteAuctionTest {

		@Test
		@DisplayName("경매 삭제 성공")
		void success_deleteAuction() {
			// given
			Long auctionId = 1L;

			given(userDomainService.findActiveUserById(authUser.getId())).willReturn(seller);
			given(auctionDomainService.deleteAuction(auctionId, seller)).willReturn(auctionId);
			willDoNothing().given(auctionSchedulerService).cancelSchedule(auctionId);

			// when
			DeleteAuctionResponse result = auctionService.deleteAuction(auctionId, authUser);

			// then
			assertThat(result.auctionId()).isEqualTo(auctionId);
			verify(userDomainService).findActiveUserById(authUser.getId());
			verify(auctionDomainService).deleteAuction(auctionId, seller);
			verify(auctionSchedulerService).cancelSchedule(auctionId);
		}

		@Test
		@DisplayName("경매 삭제 실패 - 사용자를 찾을 수 없음")
		void fail_deleteAuction_userNotFound() {
			// given
			Long auctionId = 1L;

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> auctionService.deleteAuction(auctionId, authUser))
				.isInstanceOf(UserException.class)
				.extracting("errorCode")
				.isEqualTo(UserErrorCode.USER_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("수동 경매 종료")
	class ManualEndAuctionTest {

		@Test
		@DisplayName("수동 경매 종료 성공")
		void success_manualEndAuction() {
			// given
			Long auctionId = 1L;
			Long bidId = 100L;
			Long productId = 1L;
			ManualEndAuctionRequest request = new ManualEndAuctionRequest(bidId, productId);

			Bid wonBid = createBid(bidId, bidder, CURRENT_PRICE);
			ManualEndAuctionInfo auctionInfo = createManualEndAuctionInfo(auctionId, bidId);

			given(userDomainService.findActiveUserById(authUser.getId())).willReturn(seller);
			given(bidDomainService.findBid(bidId)).willReturn(wonBid);
			given(productDomainService.findActiveProductById(productId)).willReturn(product);
			given(auctionDomainService.manualEndAuction(seller, wonBid, auctionId)).willReturn(auctionInfo);

			// when
			ManualEndAuctionResponse result = auctionService.manualEndAuction(auctionId, authUser, request);

			// then
			assertThat(result.auctionId()).isEqualTo(auctionId);
			assertThat(result.winnerNickname()).isEqualTo(BIDDER_NICKNAME);
			assertThat(result.bidId()).isEqualTo(bidId);
			assertThat(result.wonProductPrice()).isEqualTo(CURRENT_PRICE);
			assertThat(result.wonProductName()).isEqualTo(PRODUCT_NAME);
		}

		@Test
		@DisplayName("수동 경매 종료 실패 - 입찰을 찾을 수 없음")
		void fail_manualEndAuction_bidNotFound() {
			// given
			Long auctionId = 1L;
			Long bidId = 100L;
			Long productId = 1L;
			ManualEndAuctionRequest request = new ManualEndAuctionRequest(bidId, productId);

			given(userDomainService.findActiveUserById(authUser.getId())).willReturn(seller);
			given(bidDomainService.findBid(bidId)).willThrow(new BidException(BidErrorCode.BID_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> auctionService.manualEndAuction(auctionId, authUser, request))
				.isInstanceOf(BidException.class)
				.extracting("errorCode")
				.isEqualTo(BidErrorCode.BID_NOT_FOUND);
		}

		@Test
		@DisplayName("수동 경매 종료 실패 - 상품을 찾을 수 없음")
		void fail_manualEndAuction_productNotFound() {
			// given
			Long auctionId = 1L;
			Long bidId = 100L;
			Long productId = 1L;
			ManualEndAuctionRequest request = new ManualEndAuctionRequest(bidId, productId);

			Bid wonBid = createBid(bidId, bidder, CURRENT_PRICE);

			given(userDomainService.findActiveUserById(authUser.getId())).willReturn(seller);
			given(bidDomainService.findBid(bidId)).willReturn(wonBid);
			given(productDomainService.findActiveProductById(productId))
				.willThrow(new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> auctionService.manualEndAuction(auctionId, authUser, request))
				.isInstanceOf(ProductException.class)
				.extracting("errorCode")
				.isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("경매 상세 조회")
	class FindAuctionTest {

		@Test
		@DisplayName("경매 상세 조회 성공")
		void success_findAuction() {
			// given
			Long auctionId = 1L;
			Bid highestBid = createBid(100L, bidder, CURRENT_PRICE);
			AuctionFindDetailInfo detailInfo = createAuctionFindDetailInfo(auctionId);

			given(bidDomainService.findHighBidByAuction(auctionId)).willReturn(highestBid);
			given(auctionDomainService.findAuction(auctionId)).willReturn(detailInfo);

			// when
			FindDetailAuctionResponse result = auctionService.findAuction(auctionId);

			// then
			assertThat(result.auctionId()).isEqualTo(auctionId);
			assertThat(result.productName()).isEqualTo(PRODUCT_NAME);
			assertThat(result.sellerNickname()).isEqualTo(SELLER_NICKNAME);
			assertThat(result.startPrice()).isEqualTo(START_PRICE);
			assertThat(result.currentPrice()).isEqualTo(CURRENT_PRICE);
			assertThat(result.bidUserId()).isEqualTo(bidder.getId());
			assertThat(result.bidUserNickname()).isEqualTo(BIDDER_NICKNAME);
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
		Product product = spy(Product.builder()
			.name(name)
			.description("상품 설명")
			.price(START_PRICE)
			.txMethod(ProductTxMethod.AUCTION)
			.seller(seller)
			.catalog(catalog)
			.build());
		ReflectionTestUtils.setField(product, "id", id);
		return product;
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

	private List<AuctionFindAllInfo> createAuctionFindAllInfoList() {
		AuctionFindAllInfo info1 = new AuctionFindAllInfo(
			1L,
			START_PRICE,
			110000L,
			false,
			LocalDateTime.now().plusDays(1),
			LocalDateTime.now(),
			PRODUCT_NAME,
			"image.jpg",
			10L);

		AuctionFindAllInfo info2 = new AuctionFindAllInfo(
			2L,
			120000L,
			125000L,
			false,
			LocalDateTime.now().plusDays(1),
			LocalDateTime.now(),
			PRODUCT_NAME,
			"image.jpg",
			5L);

		return List.of(info1, info2);
	}

	private AuctionFindDetailInfo createAuctionFindDetailInfo(Long auctionId) {
		return new AuctionFindDetailInfo(
			auctionId,
			1L,
			SELLER_NICKNAME,
			SELLER_EMAIL,
			START_PRICE,
			CURRENT_PRICE,
			false,
			LocalDateTime.now().plusDays(1),
			7L,
			PRODUCT_NAME,
			"image.jpg",
			LocalDateTime.now().minusDays(1),
			3L);
	}

	private ManualEndAuctionInfo createManualEndAuctionInfo(Long auctionId, Long bidId) {
		return new ManualEndAuctionInfo(
			auctionId,
			bidId,
			2L,
			BIDDER_NICKNAME,
			BIDDER_EMAIL,
			CURRENT_PRICE,
			PRODUCT_NAME,
			LocalDateTime.now());
	}
}

