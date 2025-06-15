package nbc.chillguys.nebulazone.domain.auction.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
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
import nbc.chillguys.nebulazone.domain.auction.repository.AuctionRepository;
import nbc.chillguys.nebulazone.domain.bid.entity.Bid;
import nbc.chillguys.nebulazone.domain.bid.entity.BidStatus;
import nbc.chillguys.nebulazone.domain.catalog.entity.Catalog;
import nbc.chillguys.nebulazone.domain.catalog.entity.CatalogType;
import nbc.chillguys.nebulazone.domain.products.entity.Product;
import nbc.chillguys.nebulazone.domain.products.entity.ProductTxMethod;
import nbc.chillguys.nebulazone.domain.user.entity.OAuthType;
import nbc.chillguys.nebulazone.domain.user.entity.User;
import nbc.chillguys.nebulazone.domain.user.entity.UserRole;

@DisplayName("자동 경매 도메인 서비스 단위 테스트")
@ExtendWith({MockitoExtension.class})
class AutoAuctionDomainServiceUnitTest {

	@Mock
	AuctionRepository auctionRepository;

	@InjectMocks
	AutoAuctionDomainService autoAuctionDomainService;

	private static final String SELLER_EMAIL = "seller@test.com";
	private static final String SELLER_NICKNAME = "판매자";
	private static final String BUYER_EMAIL = "buyer@test.com";
	private static final String BUYER_NICKNAME = "구매자";
	private static final String PRODUCT_NAME = "테스트 CPU";
	private static final String PRODUCT_DESCRIPTION = "설명";
	private static final String CATALOG_NAME = "테스트 CPU";
	private static final String CATALOG_DESCRIPTION = "카탈로그";
	private static final Long START_PRICE = 100000L;
	private static final Long BID_PRICE = 150000L;

	private User seller;
	private User buyer;
	private Catalog catalog;
	private Product product;

	@BeforeEach
	void setUp() {
		seller = createUser(1L, SELLER_EMAIL, SELLER_NICKNAME);
		buyer = createUser(2L, BUYER_EMAIL, BUYER_NICKNAME);
		catalog = createCatalog(1L, CATALOG_NAME, CATALOG_DESCRIPTION);
		product = createProduct(1L, PRODUCT_NAME, PRODUCT_DESCRIPTION, START_PRICE, seller, catalog);
	}

	@Nested
	@DisplayName("자동 경매 종료")
	class AutoEndAuctionTest {

		@Test
		@DisplayName("자동 경매 종료 성공 - 낙찰")
		void success_endAutoAuction_bidWon() {
			// given
			Long auctionId = 1L;
			Auction auction = createAuction(auctionId, product, START_PRICE, BID_PRICE,
				LocalDateTime.now().minusHours(1), false, false);

			Bid wonBid = createBid(1L, auction, buyer, BID_PRICE);

			given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));

			// when
			autoAuctionDomainService.endAutoAuction(auctionId, wonBid);

			// then
			assertThat(auction.isWon()).isTrue();
			assertThat(wonBid.getStatus()).isEqualTo(BidStatus.WON);
		}

		@Test
		@DisplayName("자동 경매 종료 성공 - 유찰")
		void success_endAutoAuction_bidFailed() {
			// given
			Long auctionId = 1L;
			Auction auction = createAuction(auctionId, product, START_PRICE, START_PRICE,
				LocalDateTime.now().minusHours(1), false, false);

			given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));

			// when
			autoAuctionDomainService.endAutoAuction(auctionId, null);

			// then
			assertThat(auction.isWon()).isFalse();
		}

		@Test
		@DisplayName("자동 경매 종료 실패 - 이미 낙찰된 경매")
		void fail_endAutoAuction_alreadyWon() {
			// given
			Long auctionId = 1L;
			Auction alreadyWonAuction = createAuction(auctionId, product, START_PRICE, BID_PRICE,
				LocalDateTime.now().minusHours(1), false, true);
			Bid newBid = createBid(2L, alreadyWonAuction, buyer, 200000L);

			given(auctionRepository.findById(auctionId)).willReturn(Optional.of(alreadyWonAuction));

			// when
			autoAuctionDomainService.endAutoAuction(auctionId, newBid);

			// then
			assertThat(alreadyWonAuction.isWon()).isTrue();
			assertThat(newBid.getStatus()).isEqualTo(BidStatus.BID);
		}

		@Test
		@DisplayName("자동 경매 종료 실패 - 삭제된 경매")
		void fail_endAutoAuction_alreadyDeleted() {
			// given
			Long auctionId = 1L;
			Auction deletedAuction = createDeletedAuction(auctionId, product, START_PRICE, BID_PRICE);
			Bid wonBid = createBid(1L, deletedAuction, buyer, BID_PRICE);

			given(auctionRepository.findById(auctionId)).willReturn(Optional.of(deletedAuction));

			// when
			autoAuctionDomainService.endAutoAuction(auctionId, wonBid);

			// then
			assertThat(deletedAuction.isWon()).isFalse();
			assertThat(wonBid.getStatus()).isEqualTo(BidStatus.BID);
		}

		@Test
		@DisplayName("자동 경매 종료 실패 - 낙찰과 삭제가 모두 된 경매")
		void fail_endAutoAuction_wonAndDeleted() {
			// given
			Long auctionId = 1L;
			Auction wonAndDeletedAuction = createWonAndDeletedAuction(auctionId, product, START_PRICE, BID_PRICE);
			Bid wonBid = createBid(1L, wonAndDeletedAuction, buyer, BID_PRICE);

			given(auctionRepository.findById(auctionId)).willReturn(Optional.of(wonAndDeletedAuction));

			// when
			autoAuctionDomainService.endAutoAuction(auctionId, wonBid);

			// then
			assertThat(wonAndDeletedAuction.isWon()).isTrue();
			assertThat(wonAndDeletedAuction.isDeleted()).isTrue();
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
		Product product = Product.builder()
			.name(name)
			.description(description)
			.price(price)
			.txMethod(ProductTxMethod.AUCTION)
			.seller(seller)
			.catalog(catalog)
			.build();
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

	private Auction createDeletedAuction(Long id, Product product, Long startPrice, Long currentPrice) {
		Auction auction = createAuction(id, product, startPrice, currentPrice,
			LocalDateTime.now().plusDays(1), true, false);
		ReflectionTestUtils.setField(auction, "deletedAt", LocalDateTime.now());
		return auction;
	}

	private Auction createWonAndDeletedAuction(Long id, Product product, Long startPrice, Long currentPrice) {
		Auction auction = createAuction(id, product, startPrice, currentPrice,
			LocalDateTime.now().plusDays(1), true, true);
		ReflectionTestUtils.setField(auction, "deletedAt", LocalDateTime.now());
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
