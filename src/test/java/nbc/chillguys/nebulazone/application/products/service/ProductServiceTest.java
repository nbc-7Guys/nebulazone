package nbc.chillguys.nebulazone.application.products.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

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
import org.springframework.web.multipart.MultipartFile;

import nbc.chillguys.nebulazone.application.auction.service.AuctionSchedulerService;
import nbc.chillguys.nebulazone.application.products.dto.request.CreateProductRequest;
import nbc.chillguys.nebulazone.application.products.dto.response.ProductResponse;
import nbc.chillguys.nebulazone.application.products.dto.response.SearchProductResponse;
import nbc.chillguys.nebulazone.domain.auction.dto.AuctionCreateCommand;
import nbc.chillguys.nebulazone.domain.auction.entity.Auction;
import nbc.chillguys.nebulazone.domain.auction.service.AuctionDomainService;
import nbc.chillguys.nebulazone.domain.auth.vo.AuthUser;
import nbc.chillguys.nebulazone.domain.catalog.entity.Catalog;
import nbc.chillguys.nebulazone.domain.catalog.exception.CatalogErrorCode;
import nbc.chillguys.nebulazone.domain.catalog.exception.CatalogException;
import nbc.chillguys.nebulazone.domain.catalog.service.CatalogDomainService;
import nbc.chillguys.nebulazone.domain.products.dto.ProductCreateCommand;
import nbc.chillguys.nebulazone.domain.products.dto.ProductFindQuery;
import nbc.chillguys.nebulazone.domain.products.dto.ProductSearchCommand;
import nbc.chillguys.nebulazone.domain.products.entity.Product;
import nbc.chillguys.nebulazone.domain.products.entity.ProductEndTime;
import nbc.chillguys.nebulazone.domain.products.entity.ProductTxMethod;
import nbc.chillguys.nebulazone.domain.products.service.ProductDomainService;
import nbc.chillguys.nebulazone.domain.products.vo.ProductDocument;
import nbc.chillguys.nebulazone.domain.transaction.service.TransactionDomainService;
import nbc.chillguys.nebulazone.domain.user.entity.Address;
import nbc.chillguys.nebulazone.domain.user.entity.OAuthType;
import nbc.chillguys.nebulazone.domain.user.entity.User;
import nbc.chillguys.nebulazone.domain.user.entity.UserRole;
import nbc.chillguys.nebulazone.domain.user.exception.UserErrorCode;
import nbc.chillguys.nebulazone.domain.user.exception.UserException;
import nbc.chillguys.nebulazone.domain.user.service.UserDomainService;
import nbc.chillguys.nebulazone.infra.aws.s3.S3Service;

@DisplayName("상품 애플리케이션 서비스 단위 테스트")
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
	@Mock
	private UserDomainService userDomainService;

	@Mock
	private ProductDomainService productDomainService;

	@Mock
	private AuctionDomainService auctionDomainService;

	@Mock
	private TransactionDomainService transactionDomainService;

	@Mock
	private AuctionSchedulerService auctionSchedulerService;

	@Mock
	private CatalogDomainService catalogDomainService;

	@Mock
	private S3Service s3Service;

	@InjectMocks
	private ProductService productService;

	private User user;
	private Catalog catalog;
	private Product product;
	private Product auctionProduct;
	private Auction auction;
	private AuthUser authUser;

	@BeforeEach
	void init() {
		HashSet<Address> addresses = new HashSet<>();

		IntStream.range(1, 4)
			.forEach(i -> addresses.add(
				Address.builder()
					.addressNickname("테스트 주소 닉네임" + i)
					.roadAddress("도로명 주소 테스트" + i)
					.detailAddress("상세 주소 테스트" + i)
					.build()
			));

		user = User.builder()
			.email("test@test.com")
			.password("password")
			.phone("01012345678")
			.nickname("테스트닉")
			.profileImage("test.jpg")
			.point(0)
			.oAuthType(OAuthType.DOMAIN)
			.roles(Set.of(UserRole.ROLE_USER))
			.addresses(addresses)
			.build();

		ReflectionTestUtils.setField(user, "id", 1L);

		catalog = Catalog.builder()
			.build();

		ReflectionTestUtils.setField(catalog, "id", 1L);

		product = Product.builder()
			.name("일반 판매글 제목1")
			.description("일반 판매글 내용1")
			.price(2_000_000L)
			.txMethod(ProductTxMethod.DIRECT)
			.seller(user)
			.catalog(catalog)
			.build();

		ReflectionTestUtils.setField(product, "id", 1L);

		auctionProduct = Product.builder()
			.name("경매 판매글 제목1")
			.description("경매 판매글 내용1")
			.price(2_000_000L)
			.txMethod(ProductTxMethod.AUCTION)
			.seller(user)
			.catalog(catalog)
			.build();

		ReflectionTestUtils.setField(auctionProduct, "id", 2L);

		auction = Auction.builder()
			.product(auctionProduct)
			.startPrice(30000L)
			.currentPrice(30000L)
			.endTime(LocalDateTime.now().plusHours(12))
			.isWon(false)
			.build();

		ReflectionTestUtils.setField(auction, "id", 1L);

		authUser = AuthUser.builder()
			.id(1L)
			.email("test@test.com")
			.roles(Set.of(UserRole.ROLE_USER))
			.build();
	}

	@Nested
	@DisplayName("상품 검색 테스트")
	class SearchProductTest {
		@Test
		@DisplayName("상품 검색 성공")
		void success_searchProduct() {
			// Given
			ProductSearchCommand command = ProductSearchCommand.of("테스트", ProductTxMethod.DIRECT,
				0L, 10000L, 1, 10);

			given(productDomainService.searchProduct(command))
				.willReturn(new PageImpl<>(List.of(ProductDocument.from(product)),
					PageRequest.of(0, 10), 1L));

			// When
			Page<SearchProductResponse> responses = productService.searchProduct("테스트", ProductTxMethod.DIRECT,
				0L, 10000L, 1, 10);

			// Then
			assertThat(responses.getContent()).hasSize(1);

			verify(productDomainService, times(1)).searchProduct(command);

		}
	}

	@Nested
	@DisplayName("상품 조회 테스트")
	class GetProductTest {
		@Test
		@DisplayName("상품 조회 성공")
		void success_getProduct() {
			// Given
			Long productId = 1L;
			Long catalogId = 1L;
			ProductFindQuery query = new ProductFindQuery(catalogId, productId);

			given(catalogDomainService.getCatalogById(catalogId))
				.willReturn(catalog);
			given(productDomainService.getProductByIdWithUserAndImages(query))
				.willReturn(product);

			// When
			ProductResponse response = productService.getProduct(catalogId, productId);

			// Then
			assertThat(response.productId()).isEqualTo(productId);
			assertThat(response.productName()).isEqualTo(product.getName());
			assertThat(response.productDescription()).isEqualTo(product.getDescription());
			assertThat(response.productPrice()).isEqualTo(product.getPrice());

			verify(productDomainService, times(1)).getProductByIdWithUserAndImages(query);

		}
	}

	@Nested
	@DisplayName("상품 생성 테스트")
	class CreateProductTest {

		@Test
		@DisplayName("상품 생성 성공 - 즉시거래")
		void success_createProduct_direct() {
			// given
			Long catalogId = 1L;
			CreateProductRequest request = new CreateProductRequest(
				product.getName(),
				product.getDescription(),
				"direct",
				50000L,
				null
			);
			List<MultipartFile> files = List.of();

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willReturn(user);
			given(catalogDomainService.getCatalogById(catalogId))
				.willReturn(catalog);
			given(productDomainService.createProduct(any(ProductCreateCommand.class), any(List.class)))
				.willReturn(product);

			// When
			ProductResponse result = productService.createProduct(authUser, catalogId, request, files);

			// Then
			assertThat(result.productId()).isEqualTo(1L);
			assertThat(result.productName()).isEqualTo("일반 판매글 제목1");
			assertThat(result.productPrice()).isEqualTo(2_000_000L);
			assertThat(result.productTxMethod()).isEqualTo(ProductTxMethod.DIRECT);
			assertThat(result.endTime()).isNull();

			verify(userDomainService, times(1)).findActiveUserById(authUser.getId());
			verify(catalogDomainService, times(1)).getCatalogById(catalogId);
			verify(productDomainService, times(1)).createProduct(any(ProductCreateCommand.class), any(List.class));
			verify(auctionDomainService, never()).createAuction(any());
			verify(auctionSchedulerService, never()).autoAuctionEndSchedule(any(), any());
		}

		@Test
		@DisplayName("상품 생성 성공 - 경매")
		void success_createProduct_auction() {
			// Given
			Long catalogId = 1L;
			CreateProductRequest request = new CreateProductRequest(
				auctionProduct.getName(),
				auctionProduct.getDescription(),
				"auction",
				30000L,
				"hour_12"
			);
			List<MultipartFile> files = List.of();

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willReturn(user);
			given(catalogDomainService.getCatalogById(catalogId))
				.willReturn(catalog);
			given(productDomainService.createProduct(any(ProductCreateCommand.class), any(List.class)))
				.willReturn(auctionProduct);
			given(auctionDomainService.createAuction(any(AuctionCreateCommand.class)))
				.willReturn(auction);

			// When
			ProductResponse result = productService.createProduct(authUser, catalogId, request, files);

			// Then
			assertThat(result.productId()).isEqualTo(2L);
			assertThat(result.productName()).isEqualTo("경매 판매글 제목1");
			assertThat(result.productPrice()).isEqualTo(2_000_000L);
			assertThat(result.productTxMethod()).isEqualTo(ProductTxMethod.AUCTION);
			assertThat(result.endTime()).isEqualTo(ProductEndTime.HOUR_12);

			verify(userDomainService, times(1)).findActiveUserById(authUser.getId());
			verify(catalogDomainService, times(1)).getCatalogById(catalogId);
			verify(productDomainService, times(1)).createProduct(any(ProductCreateCommand.class), any(List.class));
			verify(auctionDomainService, times(1)).createAuction(any(AuctionCreateCommand.class));
			verify(auctionSchedulerService, times(1)).autoAuctionEndSchedule(auction, auctionProduct.getId());
		}

		@Test
		@DisplayName("상품 생성 실패 - 존재하지 않는 유저")
		void fail_createProduct_userNotFound() {
			// Given
			Long catalogId = 1L;
			CreateProductRequest request = new CreateProductRequest(
				"테스트 상품",
				"테스트 상품 설명",
				"direct",
				50000L,
				null
			);
			List<MultipartFile> files = List.of();

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

			// When & Then
			assertThatThrownBy(() -> productService.createProduct(authUser, catalogId, request, files))
				.isInstanceOf(UserException.class)
				.hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);

			verify(userDomainService, times(1)).findActiveUserById(authUser.getId());
		}

		@Test
		@DisplayName("상품 생성 실패 - 존재하지 않는 카탈로그")
		void fail_createProduct_catalogNotFound() {
			// Given
			Long catalogId = 999L;
			CreateProductRequest request = new CreateProductRequest(
				"테스트 상품",
				"테스트 상품 설명",
				"direct",
				50000L,
				null
			);
			List<MultipartFile> files = List.of();

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willReturn(user);
			given(catalogDomainService.getCatalogById(catalogId))
				.willThrow(new CatalogException(CatalogErrorCode.CATALOG_NOT_FOUND));

			// When & Then
			assertThatThrownBy(() -> productService.createProduct(authUser, catalogId, request, files))
				.isInstanceOf(CatalogException.class)
				.hasFieldOrPropertyWithValue("errorCode", CatalogErrorCode.CATALOG_NOT_FOUND);

			verify(userDomainService, times(1)).findActiveUserById(authUser.getId());
			verify(catalogDomainService, times(1)).getCatalogById(catalogId);
			verify(productDomainService, never()).createProduct(any(), any());
		}
	}

}
