package nbc.chillguys.nebulazone.application.product.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import nbc.chillguys.nebulazone.application.product.dto.request.CreateProductRequest;
import nbc.chillguys.nebulazone.application.product.dto.response.ProductResponse;
import nbc.chillguys.nebulazone.application.product.dto.response.SearchProductResponse;
import nbc.chillguys.nebulazone.application.product.service.ProductService;
import nbc.chillguys.nebulazone.config.TestSecurityConfig;
import nbc.chillguys.nebulazone.domain.product.entity.ProductEndTime;
import nbc.chillguys.nebulazone.domain.product.entity.ProductTxMethod;
import nbc.chillguys.nebulazone.support.MockMvc.TestMockConfig;
import nbc.chillguys.nebulazone.support.mockuser.WithCustomMockUser;

@Import({TestSecurityConfig.class, TestMockConfig.class})
@DisplayName("상품 컨트롤러 단위 테스트")
@WebMvcTest(ProductController.class)
class ProductControllerTest {
	@MockitoBean
	private ProductService productService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Nested
	@DisplayName("상품 생성")
	class CreateProductTest {

		@Test
		@DisplayName("상품 생성 성공 - 경매")
		@WithCustomMockUser
		void success_createProduct_auction() throws Exception {
			// Given
			Long catalogId = 1L;
			CreateProductRequest request = new CreateProductRequest(
				"테스트 경매 상품",
				"테스트 경매 상품 설명",
				"auction",
				1500000L,
				"hour_12");

			LocalDateTime fixedCreatedAt = LocalDateTime.of(2025, 6, 13, 15, 30, 0);
			LocalDateTime fixedModifiedAt = LocalDateTime.of(2025, 6, 13, 15, 30, 0);
			ProductResponse expectedResponse = new ProductResponse(1L,
				"테스트 경매 상품",
				"테스트 경매 상품 설명",
				1500000L,
				ProductTxMethod.AUCTION,
				false,
				ProductEndTime.HOUR_12,
				fixedCreatedAt,
				fixedModifiedAt,
				List.of("auction_image1.jpg", "auction_image2.jpg"));

			given(productService.createProduct(any(), eq(catalogId), any(CreateProductRequest.class), any()))
				.willReturn(expectedResponse);

			// When
			MockPart productPart = new MockPart("product", objectMapper.writeValueAsBytes(request));
			productPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);

			MockPart imagePart1 = new MockPart(
				"images",
				"auction_image1.jpg",
				"test file content".getBytes());
			imagePart1.getHeaders().setContentType(MediaType.IMAGE_JPEG);

			MockPart imagePart2 = new MockPart(
				"images",
				"auction_image2.jpg",
				"test file content".getBytes());
			imagePart2.getHeaders().setContentType(MediaType.IMAGE_JPEG);

			ResultActions perform = mockMvc.perform(multipart("/catalogs/{catalogId}/products", catalogId)
				.part(productPart)
				.part(imagePart1)
				.part(imagePart2));

			// Then
			perform.andDo(print())
				.andExpectAll(
					status().isCreated(),
					jsonPath("$.productId").value(1L),
					jsonPath("$.productName").value("테스트 경매 상품"),
					jsonPath("$.productDescription").value("테스트 경매 상품 설명"),
					jsonPath("$.productPrice").value(1500000L),
					jsonPath("$.productTxMethod").value(ProductTxMethod.AUCTION.name()),
					jsonPath("$.endTime").value(ProductEndTime.HOUR_12.name()),
					jsonPath("$.createdAt").value("2025-06-13 15:30:00"),
					jsonPath("$.modifiedAt").value("2025-06-13 15:30:00"),
					jsonPath("$.productImageUrls").isArray(),
					jsonPath("$.productImageUrls.length()").value(2)
				);
		}

		@Test
		@DisplayName("상품 생성 성공 - 즉시거래")
		@WithCustomMockUser
		void success_createProduct_direct() throws Exception {
			// Given
			Long catalogId = 1L;
			CreateProductRequest request = new CreateProductRequest("테스트 즉시거래 상품", "테스트 즉시거래 상품 설명", "direct",
				800000L, null);

			LocalDateTime fixedCreatedAt = LocalDateTime.of(2025, 6, 13, 16, 0, 0);
			LocalDateTime fixedModifiedAt = LocalDateTime.of(2025, 6, 13, 16, 0, 0);

			ProductResponse expectedResponse = new ProductResponse(2L, "테스트 즉시거래 상품", "테스트 즉시거래 상품 설명",
				800000L, ProductTxMethod.DIRECT, false, null, fixedCreatedAt, fixedModifiedAt,
				List.of("direct_image1.jpg"));

			given(productService.createProduct(any(), eq(catalogId), any(CreateProductRequest.class), any()))
				.willReturn(expectedResponse);

			// When
			MockPart productPart = new MockPart("product", objectMapper.writeValueAsBytes(request));
			productPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);

			MockPart imagePart = new MockPart("images", "direct_image1.jpg", "test file content".getBytes());
			imagePart.getHeaders().setContentType(MediaType.IMAGE_JPEG);

			ResultActions perform = mockMvc.perform(multipart("/catalogs/{catalogId}/products", catalogId)
				.part(productPart)
				.part(imagePart));

			// Then
			perform.andDo(print())
				.andExpectAll(
					status().isCreated(),
					jsonPath("$.productId").value(2L),
					jsonPath("$.productName").value("테스트 즉시거래 상품"),
					jsonPath("$.productDescription").value("테스트 즉시거래 상품 설명"),
					jsonPath("$.productPrice").value(800000L),
					jsonPath("$.productTxMethod").value(ProductTxMethod.DIRECT.name()),
					jsonPath("$.endTime").isEmpty(),
					jsonPath("$.createdAt").value("2025-06-13 16:00:00"),
					jsonPath("$.modifiedAt").value("2025-06-13 16:00:00"),
					jsonPath("$.productImageUrls").isArray(),
					jsonPath("$.productImageUrls.length()").value(1)
				);
		}
	}

	@Nested
	@DisplayName("상품 검색")
	class SearchProductTest {

		@Test
		@DisplayName("상품 검색 성공")
		void success_searchProduct() throws Exception {
			// Given
			SearchProductResponse response = new SearchProductResponse(1L, "testProduct",
				1L, 1L, "testSellerNickname", null, false, 1000L,
				ProductTxMethod.DIRECT.name(), LocalDateTime.now(), List.of());
			Page<SearchProductResponse> page = new PageImpl<>(List.of(response),
				PageRequest.of(0, 10), 1);

			given(productService.searchProduct(anyString(), anyString(), any(), any(), any(), anyInt(), anyInt()))
				.willReturn(page);

			// When
			ResultActions perform = mockMvc.perform(get("/products")
				.param("productname", "testProduct")
				.param("sellernickname", "testSellerNickname")
				.param("type", ProductTxMethod.DIRECT.name())
				.param("from", "1000")
				.param("to", "2000")
				.param("page", "1")
				.param("size", "10")
			);

			// Then
			perform.andDo(print())
				.andExpectAll(
					status().isOk(),
					jsonPath("$.content[0].productId")
						.value(1L),
					jsonPath("$.content[0].productName")
						.value("testProduct"),
					jsonPath("$.content[0].sellerNickname")
						.value("testSellerNickname"),
					jsonPath("$.content[0].productPrice")
						.value(1000L),
					jsonPath("$.content[0].txMethod")
						.value(ProductTxMethod.DIRECT.name()),
					jsonPath("$.page")
						.value(1),
					jsonPath("$.totalElements")
						.value(1)
				);
		}
	}

	@Nested
	@DisplayName("상품 조회")
	class GetProductTest {

		@Test
		@DisplayName("상품 조회 성공")
		void success_getProduct() throws Exception {
			// Given
			Long catalogId = 1L;
			Long productId = 1L;
			ProductResponse response = new ProductResponse(productId, "testProduct", "testDescription",
				1000L, ProductTxMethod.DIRECT, false, ProductEndTime.HOUR_12, LocalDateTime.now(), LocalDateTime.now(),
				List.of());

			given(productService.getProduct(catalogId, productId)).willReturn(response);

			// When & Then
			ResultActions perform = mockMvc.perform(
				get("/catalogs/{catalogId}/products/{productId}", catalogId, productId));

			perform.andDo(print())
				.andExpectAll(
					status().isOk(),
					jsonPath("$.productId")
						.value(productId),
					jsonPath("$.productName")
						.value("testProduct"),
					jsonPath("$.productPrice")
						.value(1000L),
					jsonPath("$.productTxMethod")
						.value(ProductTxMethod.DIRECT.name())
				);
		}
	}

}
