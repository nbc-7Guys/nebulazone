package nbc.chillguys.nebulazone.application.auction.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import nbc.chillguys.nebulazone.domain.auction.entity.Auction;
import nbc.chillguys.nebulazone.domain.auction.exception.AuctionErrorCode;
import nbc.chillguys.nebulazone.domain.auction.exception.AuctionException;
import nbc.chillguys.nebulazone.domain.auction.service.AuctionDomainService;
import nbc.chillguys.nebulazone.domain.auction.service.AutoAuctionDomainService;
import nbc.chillguys.nebulazone.domain.bid.entity.Bid;
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

@DisplayName("자동 경매 서비스 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AutoAuctionServiceUnitTest {

	@Mock
	AuctionDomainService auctionDomainService;

	@Mock
	TransactionDomainService txDomainService;

	@Mock
	ProductDomainService productDomainService;

	@Mock
	AutoAuctionDomainService autoAuctionDomainService;

	@Mock
	BidDomainService bidDomainService;

	@InjectMocks
	AutoAuctionService autoAuctionService;

	private static final String SELLER_EMAIL = "seller@test.com";
	private static final String SELLER_NICKNAME = "판매자";
	private static final String BIDDER_EMAIL = "bidder@test.com";
	private static final String BIDDER_NICKNAME = "입찰자";
	private static final String PRODUCT_NAME = "테스트 상품";
	private static final String PRODUCT_DESCRIPTION = "상품 설명";
	private static final String CATALOG_NAME = "테스트 카탈로그";
	private static final String CATALOG_DESCRIPTION = "카탈로그 설명";
	private static final Long START_PRICE = 100000L;
	private static final Long CURRENT_PRICE = 150000L;

	private User seller;
	private User bidder;
	private Catalog catalog;
	private Product product;
	private Auction auction;
	private Bid wonBid;

	@BeforeEach
	void setUp() {
		seller = createUser(1L, SELLER_EMAIL, SELLER_NICKNAME);
		bidder = createUser(2L, BIDDER_EMAIL, BIDDER_NICKNAME);
		catalog = createCatalog(1L, CATALOG_NAME, CATALOG_DESCRIPTION);
		product = createProduct(1L, PRODUCT_NAME, PRODUCT_DESCRIPTION, START_PRICE, seller, catalog);
		auction = createAuction(1L, product, START_PRICE, CURRENT_PRICE, LocalDateTime.now().plusDays(1), false, false);
		wonBid = createBid(100L, auction, bidder, CURRENT_PRICE);
	}

	@Nested
	@DisplayName("자동 경매 종료 및 거래 생성")
	class AutoEndAuctionAndCreateTransactionTest {

		@Test
		@DisplayName("자동 경매 종료 및 거래 생성 성공")
		void success_autoEndAuctionAndCreateTransaction() {
			// given
			Long auctionId = auction.getId();
			Long productId = product.getId();

			given(auctionDomainService.findActiveAuctionById(auctionId)).willReturn(auction);
			given(productDomainService.findActiveProductById(productId)).willReturn(product);
			given(bidDomainService.findHighBidByAuction(auctionId)).willReturn(wonBid);

			// when
			autoAuctionService.autoEndAuctionAndCreateTransaction(auctionId, productId);

			// then
			verify(auctionDomainService).findActiveAuctionById(auctionId);
			verify(productDomainService).findActiveProductById(productId);
			verify(product).purchase();
			verify(bidDomainService).findHighBidByAuction(auctionId);
			verify(autoAuctionDomainService).endAutoAuction(auctionId, wonBid);

			// 트랜잭션 생성에 전달된 인자 검증까지!
			verify(txDomainService).createTransaction(
				argThat(cmd ->
					cmd.user().equals(wonBid.getUser()) &&
						cmd.product().equals(product) &&
						cmd.txMethod().equals(product.getTxMethod().name()) &&
						cmd.price().equals(wonBid.getPrice())
				)
			);
		}

		@Test
		@DisplayName("자동 경매 종료 및 거래 생성 성공 - 입찰이 없는 경우(유찰)")
		void success_autoEndAuctionAndCreateTransaction_noBid() {
			// given
			Long auctionId = auction.getId();
			Long productId = product.getId();

			given(auctionDomainService.findActiveAuctionById(auctionId)).willReturn(auction);
			given(productDomainService.findActiveProductById(productId)).willReturn(product);
			given(bidDomainService.findHighBidByAuction(auctionId)).willReturn(null);

			// when
			autoAuctionService.autoEndAuctionAndCreateTransaction(auctionId, productId);

			// then
			verify(autoAuctionDomainService).endAutoAuction(auctionId, null);
			verify(txDomainService, never()).createTransaction(any());
		}

		@Test
		@DisplayName("자동 경매 종료 실패 - 경매를 찾을 수 없음")
		void fail_autoEndAuctionAndCreateTransaction_auctionNotFound() {
			// given
			Long auctionId = 999L;
			Long productId = product.getId();

			given(auctionDomainService.findActiveAuctionById(auctionId))
				.willThrow(new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> autoAuctionService.autoEndAuctionAndCreateTransaction(auctionId, productId))
				.isInstanceOf(AuctionException.class)
				.extracting("errorCode")
				.isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);
		}

		@Test
		@DisplayName("자동 경매 종료 실패 - 상품을 찾을 수 없음")
		void fail_autoEndAuctionAndCreateTransaction_productNotFound() {
			// given
			Long auctionId = auction.getId();
			Long productId = 999L;

			given(auctionDomainService.findActiveAuctionById(auctionId)).willReturn(auction);
			given(productDomainService.findActiveProductById(productId))
				.willThrow(new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> autoAuctionService.autoEndAuctionAndCreateTransaction(auctionId, productId))
				.isInstanceOf(ProductException.class)
				.extracting("errorCode")
				.isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND);
		}
	}

	// 팩토리 메서드들
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

	private Catalog createCatalog(Long id, String name, String description) {
		Catalog catalog = Catalog.builder()
			.name(name)
			.description(description)
			.type(CatalogType.CPU)
			.build();
		ReflectionTestUtils.setField(catalog, "id", id);
		return catalog;
	}

	private Product createProduct(Long id, String name, String description, Long price, User seller, Catalog catalog) {
		Product product = spy(Product.builder()
			.name(name)
			.description(description)
			.price(price)
			.txMethod(ProductTxMethod.AUCTION)
			.seller(seller)
			.catalog(catalog)
			.build());
		ReflectionTestUtils.setField(product, "id", id);
		return product;
	}

	private Auction createAuction(Long id, Product product, Long startPrice, Long currentPrice,
		LocalDateTime endTime, boolean isDeleted, boolean isWon) {
		Auction auction = Auction.builder()
			.product(product)
			.startPrice(startPrice)
			.currentPrice(currentPrice)
			.endTime(endTime)
			.isDeleted(isDeleted)
			.isWon(isWon)
			.build();
		ReflectionTestUtils.setField(auction, "id", id);
		return auction;
	}

	private Bid createBid(Long id, Auction auction, User user, Long price) {
		Bid bid = Bid.builder()
			.auction(auction)
			.user(user)
			.price(price)
			.build();
		ReflectionTestUtils.setField(bid, "id", id);
		return bid;
	}
}
