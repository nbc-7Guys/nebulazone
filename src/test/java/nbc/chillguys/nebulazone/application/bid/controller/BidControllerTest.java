package nbc.chillguys.nebulazone.application.bid.controller;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import nbc.chillguys.nebulazone.application.bid.dto.request.CreateBidRequest;
import nbc.chillguys.nebulazone.application.bid.dto.response.CreateBidResponse;
import nbc.chillguys.nebulazone.application.bid.dto.response.DeleteBidResponse;
import nbc.chillguys.nebulazone.application.bid.dto.response.FindBidResponse;
import nbc.chillguys.nebulazone.application.bid.service.BidService;
import nbc.chillguys.nebulazone.common.response.CommonPageResponse;
import nbc.chillguys.nebulazone.config.TestSecurityConfig;
import nbc.chillguys.nebulazone.domain.user.entity.User;
import nbc.chillguys.nebulazone.support.MockMvc.TestMockConfig;
import nbc.chillguys.nebulazone.support.mockuser.WithCustomMockUser;

@DisplayName("입찰 컨트롤러 단위 테스트")
@WebMvcTest(BidController.class)
@Import({TestSecurityConfig.class, TestMockConfig.class})
class BidControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	BidService bidService;

	private static final Long AUCTION_ID = 1L;
	private static final Long BID_ID = 100L;
	private static final String NICKNAME = "입찰자닉네임";
	private static final String PRODUCT_NAME = "테스트 상품";
	private static final String BID_STATUS_MESSAGE = "입찰 성공";
	private static final Long BID_PRICE = 150000L;

	private final LocalDateTime bidTime = LocalDateTime.of(2024, 12, 30, 15, 30, 0);

	@Nested
	@DisplayName("입찰 생성")
	class CreateBidTest {

		@Test
		@DisplayName("입찰 생성 성공")
		@WithCustomMockUser
		void success_upsertBid() throws Exception {
			// given
			CreateBidRequest request = new CreateBidRequest(BID_PRICE);
			CreateBidResponse response = new CreateBidResponse(BID_ID, BID_PRICE);

			given(bidService.upsertBid(eq(AUCTION_ID), any(User.class), eq(request)))
				.willReturn(response);

			// when & then
			mockMvc.perform(post("/auctions/{auctionId}/bids", AUCTION_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpectAll(
					status().isCreated(),
					content().contentType(MediaType.APPLICATION_JSON),
					jsonPath("$.bidId").value(BID_ID),
					jsonPath("$.bidPrice").value(BID_PRICE)
				);
		}
	}

	@Nested
	@DisplayName("경매별 입찰 조회")
	class FindBidsTest {

		@Test
		@DisplayName("경매별 입찰 조회 성공 - 기본 페이징")
		@WithCustomMockUser
		void success_findBids() throws Exception {
			// given
			FindBidResponse bidContent1 = new FindBidResponse(
				BID_ID, NICKNAME, PRODUCT_NAME, BID_STATUS_MESSAGE, BID_PRICE, bidTime
			);
			FindBidResponse bidContent2 = new FindBidResponse(
				BID_ID + 1, NICKNAME + "2", PRODUCT_NAME, BID_STATUS_MESSAGE, BID_PRICE + 10000, bidTime.plusMinutes(10)
			);
			List<FindBidResponse> contents = List.of(bidContent1, bidContent2);

			Page<FindBidResponse> page = new PageImpl<>(contents, PageRequest.of(0, 20), 2);
			CommonPageResponse<FindBidResponse> expectedResponse = CommonPageResponse.from(page);

			given(bidService.findBids(AUCTION_ID, 0, 20)).willReturn(expectedResponse);

			// when & then
			mockMvc.perform(get("/auctions/{auctionId}/bids", AUCTION_ID))
				.andDo(print())
				.andExpectAll(
					status().isOk(),
					content().contentType(MediaType.APPLICATION_JSON),
					jsonPath("$.content").isArray(),
					jsonPath("$.content.length()").value(2),
					jsonPath("$.content[0].BidId").value(BID_ID),
					jsonPath("$.content[0].nickname").value(NICKNAME),
					jsonPath("$.content[0].productName").value(PRODUCT_NAME),
					jsonPath("$.content[0].bidStatusMessage").value(BID_STATUS_MESSAGE),
					jsonPath("$.content[0].bidPrice").value(BID_PRICE),
					jsonPath("$.content[0].bidTime").value(
						bidTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
					jsonPath("$.content[1].BidId").value(BID_ID + 1),
					jsonPath("$.content[1].nickname").value(NICKNAME + "2"),
					jsonPath("$.content[1].bidPrice").value(BID_PRICE + 10000),
					jsonPath("$.page").value(1),
					jsonPath("$.size").value(20),
					jsonPath("$.totalElements").value(2)
				);
		}

		@Test
		@DisplayName("경매별 입찰 조회 성공 - 사용자 정의 페이징")
		@WithCustomMockUser
		void success_findBids_customPaging() throws Exception {
			// given
			Page<FindBidResponse> page = new PageImpl<>(List.of(), PageRequest.of(1, 10), 0);
			CommonPageResponse<FindBidResponse> expectedResponse = CommonPageResponse.from(page);

			given(bidService.findBids(AUCTION_ID, 1, 10)).willReturn(expectedResponse);

			// when & then
			mockMvc.perform(get("/auctions/{auctionId}/bids", AUCTION_ID)
					.param("page", "2")
					.param("size", "10"))
				.andExpectAll(
					status().isOk(),
					jsonPath("$.page").value(2),
					jsonPath("$.size").value(10),
					jsonPath("$.totalElements").value(0),
					jsonPath("$.content").isEmpty()
				);
		}

		@Test
		@DisplayName("경매별 입찰 조회 성공 - 페이지 0 이하 처리")
		@WithCustomMockUser
		void success_findBids_pageZeroOrBelow() throws Exception {
			// given
			FindBidResponse bidContent = new FindBidResponse(
				BID_ID, NICKNAME, PRODUCT_NAME, BID_STATUS_MESSAGE, BID_PRICE, bidTime
			);
			List<FindBidResponse> contents = List.of(bidContent);

			Page<FindBidResponse> page = new PageImpl<>(contents, PageRequest.of(0, 20), 1);
			CommonPageResponse<FindBidResponse> expectedResponse = CommonPageResponse.from(page);

			given(bidService.findBids(AUCTION_ID, 0, 20)).willReturn(expectedResponse);

			// when & then
			mockMvc.perform(get("/auctions/{auctionId}/bids", AUCTION_ID)
					.param("page", "0")
					.param("size", "20"))
				.andExpectAll(
					status().isOk(),
					jsonPath("$.page").value(1),
					jsonPath("$.content.length()").value(1)
				);
		}

		@Test
		@DisplayName("경매별 입찰 조회 성공 - 기본값 적용")
		@WithCustomMockUser
		void success_findBids_defaultValues() throws Exception {
			// given
			Page<FindBidResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
			CommonPageResponse<FindBidResponse> expectedResponse = CommonPageResponse.from(page);

			given(bidService.findBids(AUCTION_ID, 0, 20)).willReturn(expectedResponse);

			// when & then
			mockMvc.perform(get("/auctions/{auctionId}/bids", AUCTION_ID))
				.andExpectAll(
					status().isOk(),
					jsonPath("$.page").value(1),
					jsonPath("$.size").value(20)
				);
		}
	}

	@Nested
	@DisplayName("내 입찰 조회")
	class FindMyBidsTest {

		@Test
		@DisplayName("내 입찰 조회 성공 - 기본 페이징")
		@WithCustomMockUser
		void success_findMyBids() throws Exception {
			// given
			FindBidResponse bidContent1 = new FindBidResponse(
				BID_ID, NICKNAME, PRODUCT_NAME, BID_STATUS_MESSAGE, BID_PRICE, bidTime
			);
			FindBidResponse bidContent2 = new FindBidResponse(
				BID_ID + 1, NICKNAME, PRODUCT_NAME + "2", BID_STATUS_MESSAGE, BID_PRICE + 5000, bidTime.plusMinutes(5)
			);
			List<FindBidResponse> contents = List.of(bidContent1, bidContent2);

			Page<FindBidResponse> page = new PageImpl<>(contents, PageRequest.of(0, 20), 2);
			CommonPageResponse<FindBidResponse> expectedResponse = CommonPageResponse.from(page);

			given(bidService.findMyBids(any(User.class), eq(0), eq(20))).willReturn(expectedResponse);

			// when & then
			mockMvc.perform(get("/bids/me"))
				.andDo(print())
				.andExpectAll(
					status().isOk(),
					content().contentType(MediaType.APPLICATION_JSON),
					jsonPath("$.content").isArray(),
					jsonPath("$.content.length()").value(2),
					jsonPath("$.content[0].BidId").value(BID_ID),
					jsonPath("$.content[0].nickname").value(NICKNAME),
					jsonPath("$.content[0].productName").value(PRODUCT_NAME),
					jsonPath("$.content[0].bidStatusMessage").value(BID_STATUS_MESSAGE),
					jsonPath("$.content[0].bidPrice").value(BID_PRICE),
					jsonPath("$.content[0].bidTime").value(
						bidTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
					jsonPath("$.content[1].BidId").value(BID_ID + 1),
					jsonPath("$.content[1].productName").value(PRODUCT_NAME + "2"),
					jsonPath("$.content[1].bidPrice").value(BID_PRICE + 5000),
					jsonPath("$.page").value(1),
					jsonPath("$.size").value(20),
					jsonPath("$.totalElements").value(2)
				);
		}

		@Test
		@DisplayName("내 입찰 조회 성공 - 사용자 정의 페이징")
		@WithCustomMockUser
		void success_findMyBids_customPaging() throws Exception {
			// given
			Page<FindBidResponse> page = new PageImpl<>(List.of(), PageRequest.of(2, 5), 0);
			CommonPageResponse<FindBidResponse> expectedResponse = CommonPageResponse.from(page);

			given(bidService.findMyBids(any(User.class), eq(2), eq(5))).willReturn(expectedResponse);

			// when & then
			mockMvc.perform(get("/bids/me")
					.param("page", "3")
					.param("size", "5"))
				.andExpectAll(
					status().isOk(),
					jsonPath("$.page").value(3),
					jsonPath("$.size").value(5),
					jsonPath("$.totalElements").value(0),
					jsonPath("$.content").isEmpty()
				);
		}

		@Test
		@DisplayName("내 입찰 조회 성공 - 페이지 0 이하 처리")
		@WithCustomMockUser
		void success_findMyBids_pageZeroOrBelow() throws Exception {
			// given
			FindBidResponse bidContent = new FindBidResponse(
				BID_ID, NICKNAME, PRODUCT_NAME, BID_STATUS_MESSAGE, BID_PRICE, bidTime
			);
			List<FindBidResponse> contents = List.of(bidContent);

			Page<FindBidResponse> page = new PageImpl<>(contents, PageRequest.of(0, 20), 1);
			CommonPageResponse<FindBidResponse> expectedResponse = CommonPageResponse.from(page);

			given(bidService.findMyBids(any(User.class), eq(0), eq(20))).willReturn(expectedResponse);

			// when & then
			mockMvc.perform(get("/bids/me")
					.param("page", "-1")
					.param("size", "20"))
				.andExpectAll(
					status().isOk(),
					jsonPath("$.page").value(1),
					jsonPath("$.content.length()").value(1)
				);
		}

		@Test
		@DisplayName("내 입찰 조회 성공 - 기본값 적용")
		@WithCustomMockUser
		void success_findMyBids_defaultValues() throws Exception {
			// given
			Page<FindBidResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
			CommonPageResponse<FindBidResponse> expectedResponse = CommonPageResponse.from(page);

			given(bidService.findMyBids(any(User.class), eq(0), eq(20))).willReturn(expectedResponse);

			// when & then
			mockMvc.perform(get("/bids/me"))
				.andExpectAll(
					status().isOk(),
					jsonPath("$.page").value(1),
					jsonPath("$.size").value(20)
				);
		}
	}

	@Nested
	@DisplayName("입찰 삭제(상태 변경)")
	class StatusBidTest {

		@Test
		@DisplayName("입찰 삭제 성공")
		@WithCustomMockUser
		void success_statusBid() throws Exception {
			// given
			DeleteBidResponse response = new DeleteBidResponse(BID_ID);

			given(bidService.statusBid(any(User.class), eq(AUCTION_ID), eq(BID_ID)))
				.willReturn(response);

			// when & then
			mockMvc.perform(delete("/auctions/{auctionId}/bids/{bidId}", AUCTION_ID, BID_ID))
				.andDo(print())
				.andExpectAll(
					status().isOk(),
					content().contentType(MediaType.APPLICATION_JSON),
					jsonPath("$.commentId").value(BID_ID)
				);
		}
	}
}
