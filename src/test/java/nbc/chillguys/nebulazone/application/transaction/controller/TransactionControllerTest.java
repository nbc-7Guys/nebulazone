package nbc.chillguys.nebulazone.application.transaction.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nbc.chillguys.nebulazone.application.transaction.dto.response.FindDetailTransactionResponse;
import nbc.chillguys.nebulazone.application.transaction.dto.response.FindTransactionResponse;
import nbc.chillguys.nebulazone.application.transaction.service.TransactionService;
import nbc.chillguys.nebulazone.common.response.CommonPageResponse;
import nbc.chillguys.nebulazone.config.TestSecurityConfig;
import nbc.chillguys.nebulazone.domain.transaction.entity.TransactionMethod;
import nbc.chillguys.nebulazone.domain.transaction.entity.UserType;
import nbc.chillguys.nebulazone.domain.user.entity.User;
import nbc.chillguys.nebulazone.support.MockMvc.TestMockConfig;
import nbc.chillguys.nebulazone.support.mockuser.WithCustomMockUser;

@DisplayName("거래 내역 컨트롤러 단위 테스트")
@WebMvcTest(TransactionController.class)
@Import({TestSecurityConfig.class, TestMockConfig.class})
class TransactionControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	TransactionService txService;

	private static final Long TRANSACTION_ID = 1L;
	private static final Long TX_PRICE = 150000L;
	private static final String PRODUCT_NAME = "테스트 CPU";

	private final LocalDateTime txTime1 = LocalDateTime.of(2024, 12, 30, 15, 30, 0);
	private final LocalDateTime txTime2 = LocalDateTime.of(2024, 12, 30, 20, 30, 0);

	@Nested
	@DisplayName("거래내역 조회")
	class FindTransactionTest {

		@Test
		@DisplayName("내 거래내역 전체 조회 성공 - 기본 페이징")
		@WithCustomMockUser
		void success_findMyTransactions() throws Exception {
			// given
			FindTransactionResponse txContent1 = new FindTransactionResponse(
				1L, 150000L, TransactionMethod.AUCTION, txTime1, "테스트 CPU", true,
				1L, "test", UserType.BUYER.name()
			);
			FindTransactionResponse txContent2 = new FindTransactionResponse(
				2L, 89000L, TransactionMethod.DIRECT, txTime2, "테스트 GPU", false,
				1L, "test", UserType.SELLER.name()
			);
			List<FindTransactionResponse> contents = List.of(txContent1, txContent2);

			Page<FindTransactionResponse> page = new PageImpl<>(contents, PageRequest.of(0, 20), 2);
			CommonPageResponse<FindTransactionResponse> expectedResponse = CommonPageResponse.from(page);

			given(txService.findMyTransactions(any(User.class), eq(0), eq(20)))
				.willReturn(expectedResponse);

			// when & then
			mockMvc.perform(get("/transactions/me"))
				.andDo(print())
				.andExpectAll(
					status().isOk(),
					content().contentType(MediaType.APPLICATION_JSON),
					jsonPath("$.content").isArray(),
					jsonPath("$.content.length()").value(2),
					jsonPath("$.content[0].txId").value(1L),
					jsonPath("$.content[0].txPrice").value(150000L),
					jsonPath("$.content[0].txMethod").value("AUCTION"),
					jsonPath("$.content[0].productName").value("테스트 CPU"),
					jsonPath("$.content[0].isSold").value(true),
					jsonPath("$.content[0].txCreatedAt").value(
						txTime1.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
					jsonPath("$.content[1].txId").value(2L),
					jsonPath("$.content[1].txPrice").value(89000L),
					jsonPath("$.content[1].txMethod").value("DIRECT"),
					jsonPath("$.content[1].productName").value("테스트 GPU"),
					jsonPath("$.content[1].isSold").value(false),
					jsonPath("$.page").value(1),
					jsonPath("$.size").value(20),
					jsonPath("$.totalElements").value(2)
				);
		}

		@Test
		@DisplayName("내 거래내역 전체 조회 성공 - 페이지 0 이하 처리")
		@WithCustomMockUser
		void success_findMyTransactions_pageZeroOrBelow() throws Exception {
			// given
			FindTransactionResponse txContent = new FindTransactionResponse(
				TRANSACTION_ID, TX_PRICE, TransactionMethod.AUCTION, txTime1, PRODUCT_NAME, true,
				1L, "test", UserType.BUYER.name()
			);
			List<FindTransactionResponse> contents = List.of(txContent);

			Page<FindTransactionResponse> page = new PageImpl<>(contents, PageRequest.of(0, 20), 1);
			CommonPageResponse<FindTransactionResponse> expectedResponse = CommonPageResponse.from(page);

			given(txService.findMyTransactions(any(User.class), eq(0), eq(20)))
				.willReturn(expectedResponse);

			// when & then
			mockMvc.perform(get("/transactions/me")
					.param("page", "0")
					.param("size", "20"))
				.andExpectAll(
					status().isOk(),
					jsonPath("$.page").value(1),
					jsonPath("$.content.length()").value(1)
				);
		}

		@Test
		@DisplayName("내 거래내역 상세조회 성공")
		@WithCustomMockUser
		void success_findMyTransaction() throws Exception {
			// given
			FindDetailTransactionResponse txDetail = new FindDetailTransactionResponse(
				TRANSACTION_ID, 20L, "판매자닉네임", "seller@test.com",
				250000L, txTime1, TransactionMethod.AUCTION,
				50L, "고급 그래픽카드", txTime1.minusDays(5), true
			);

			given(txService.findMyTransaction(any(User.class), eq(TRANSACTION_ID)))
				.willReturn(txDetail);

			// when & then
			mockMvc.perform(get("/transactions/{transactionId}/me", TRANSACTION_ID))
				.andDo(print())
				.andExpectAll(
					status().isOk(),
					content().contentType(MediaType.APPLICATION_JSON),
					jsonPath("$.txId").value(TRANSACTION_ID),
					jsonPath("$.sellerId").value(20L),
					jsonPath("$.sellerNickname").value("판매자닉네임"),
					jsonPath("$.sellerEmail").value("seller@test.com"),
					jsonPath("$.txPrice").value(250000L),
					jsonPath("$.txCreatedAt").value(
						txTime1.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
					jsonPath("$.txMethod").value("AUCTION"),
					jsonPath("$.productId").value(50L),
					jsonPath("$.productName").value("고급 그래픽카드"),
					jsonPath("$.productCreatedAt").value(
						txTime1.minusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
					jsonPath("$.isSold").value(true)
				);
		}
	}
}
