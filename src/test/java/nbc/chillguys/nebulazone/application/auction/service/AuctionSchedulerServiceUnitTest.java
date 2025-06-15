package nbc.chillguys.nebulazone.application.auction.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import nbc.chillguys.nebulazone.domain.catalog.entity.Catalog;
import nbc.chillguys.nebulazone.domain.catalog.entity.CatalogType;
import nbc.chillguys.nebulazone.domain.products.entity.Product;
import nbc.chillguys.nebulazone.domain.products.entity.ProductTxMethod;
import nbc.chillguys.nebulazone.domain.user.entity.OAuthType;
import nbc.chillguys.nebulazone.domain.user.entity.User;
import nbc.chillguys.nebulazone.domain.user.entity.UserRole;

@DisplayName("경매 스케줄러 서비스 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AuctionSchedulerServiceUnitTest {

	@Mock
	AuctionDomainService auctionDomainService;

	@Mock
	ScheduledExecutorService scheduler;

	@InjectMocks
	AuctionSchedulerService auctionSchedulerService;

	public static final String CATALOG = "테스트 카탈로그";
	private static final String SELLER_EMAIL = "seller@test.com";
	private static final String SELLER_NICKNAME = "판매자";
	private static final String PRODUCT_NAME = "테스트 상품";
	private static final Long START_PRICE = 100000L;
	private static final Long CURRENT_PRICE = 150000L;

	private User seller;
	private Catalog catalog;
	private Product product;
	private Map<Long, ScheduledFuture<?>> tasks;

	@BeforeEach
	void setUp() {
		seller = createUser(1L, SELLER_EMAIL, SELLER_NICKNAME);
		catalog = createCatalog(1L, CATALOG);
		product = createProduct(1L, PRODUCT_NAME, seller, catalog);
		tasks = new ConcurrentHashMap<>();

		ReflectionTestUtils.setField(auctionSchedulerService, "scheduler", scheduler);
		ReflectionTestUtils.setField(auctionSchedulerService, "tasks", tasks);
	}

	@Nested
	@DisplayName("경매 자동 종료 스케줄 등록")
	class AutoAuctionEndScheduleTest {

		@Test
		@DisplayName("스케줄 등록 성공")
		void success_autoAuctionEndSchedule() {
			// given
			LocalDateTime endTime = LocalDateTime.now().plusDays(1);
			Auction auction = createAuction(1L, product, endTime, false, false);
			Long productId = product.getId();

			// doReturn-when 패턴으로 제네릭 타입 문제 해결
			ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
			doReturn(mockFuture).when(scheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

			// when
			auctionSchedulerService.autoAuctionEndSchedule(auction, productId);

			// then
			assertThat(tasks).containsKey(auction.getId());
			assertThat(tasks).hasSize(1);
		}

		@Test
		@DisplayName("스케줄 등록 실패 - 과거 시간으로 등록")
		void fail_autoAuctionEndSchedule_pastTime() {
			// given
			LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
			Auction auction = createAuction(1L, product, pastTime, false, false);
			Long productId = product.getId();

			// when & then
			assertThatThrownBy(() -> auctionSchedulerService.autoAuctionEndSchedule(auction, productId))
				.isInstanceOf(AuctionException.class)
				.extracting("errorCode")
				.isEqualTo(AuctionErrorCode.AUCTION_END_TIME_INVALID);

			assertThat(tasks).isEmpty();
		}
	}

	@Nested
	@DisplayName("스케줄 취소")
	class CancelScheduleTest {

		@Test
		@DisplayName("스케줄 취소 성공")
		void success_cancelSchedule() {
			// given
			Long auctionId = 1L;
			ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
			tasks.put(auctionId, mockFuture);

			// when
			auctionSchedulerService.cancelSchedule(auctionId);

			// then
			assertThat(tasks).doesNotContainKey(auctionId);
			assertThat(tasks).isEmpty();
		}

		@Test
		@DisplayName("스케줄 취소 - 존재하지 않는 스케줄")
		void success_cancelSchedule_notExist() {
			// given
			Long auctionId = 999L;

			// when
			auctionSchedulerService.cancelSchedule(auctionId);

			// then
			assertThat(tasks).isEmpty();
			assertThat(tasks).doesNotContainKey(auctionId);
		}
	}

	@Nested
	@DisplayName("스케줄 복구")
	class RecoverSchedulesTest {

		@Test
		@DisplayName("스케줄 복구 성공")
		void success_recoverSchedules() {
			// given
			LocalDateTime futureTime = LocalDateTime.now().plusDays(1);
			Auction activeAuction = createAuction(1L, product, futureTime, false, false);
			Auction wonAuction = createAuction(2L, product, futureTime, false, true);

			List<Auction> auctionList = List.of(activeAuction, wonAuction);

			ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);

			given(auctionDomainService.findActiveAuctionsWithProductAndSeller()).willReturn(auctionList);
			doReturn(mockFuture).when(scheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

			// when
			auctionSchedulerService.recoverSchedules();

			// then
			assertThat(tasks).hasSize(1);
			assertThat(tasks).containsKey(activeAuction.getId());
			assertThat(tasks).doesNotContainKey(wonAuction.getId());
		}

	}

	// 팩토리 메서드들 - 적당한 추상화 유지
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
			.description("설명")
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

	private Auction createAuction(Long id, Product product, LocalDateTime endTime, boolean isDeleted, boolean isWon) {
		Auction auction = Auction.builder()
			.product(product)
			.startPrice(START_PRICE)
			.currentPrice(CURRENT_PRICE)
			.endTime(endTime)
			.isDeleted(isDeleted)
			.isWon(isWon)
			.build();
		ReflectionTestUtils.setField(auction, "id", id);
		return auction;
	}
}
