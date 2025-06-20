package nbc.chillguys.nebulazone.application.pointhistory.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import nbc.chillguys.nebulazone.application.pointhistory.dto.request.PointRequest;
import nbc.chillguys.nebulazone.application.pointhistory.dto.response.PointHistoryResponse;
import nbc.chillguys.nebulazone.application.pointhistory.dto.response.PointResponse;
import nbc.chillguys.nebulazone.application.pointhistory.service.PointHistoryService;
import nbc.chillguys.nebulazone.common.response.CommonPageResponse;
import nbc.chillguys.nebulazone.config.TestSecurityConfig;
import nbc.chillguys.nebulazone.domain.pointhistory.entity.PointHistoryStatus;
import nbc.chillguys.nebulazone.domain.pointhistory.entity.PointHistoryType;
import nbc.chillguys.nebulazone.domain.user.entity.User;
import nbc.chillguys.nebulazone.support.MockMvc.TestMockConfig;
import nbc.chillguys.nebulazone.support.mockuser.WithCustomMockUser;

@DisplayName("포인트 히스토리 컨트롤러 테스트")
@Import({TestSecurityConfig.class, TestMockConfig.class})
@WebMvcTest(PointHistoryController.class)
class PointHistoryControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@MockitoBean
	private PointHistoryService pointHistoryService;

	@Nested
	@DisplayName("포인트 내역 생성 테스트")
	class CreatePointHistoryTest {
		@Test
		@WithCustomMockUser
		@DisplayName("성공: 포인트 내역 생성")
		void success_createPointHistory() throws Exception {
			PointRequest req = new PointRequest(1000L, PointHistoryType.CHARGE, "174-598-453");
			PointResponse res = new PointResponse(1L, 1000L, PointHistoryType.CHARGE, PointHistoryStatus.PENDING,
				LocalDateTime.now());

			// GIVEN
			BDDMockito.given(pointHistoryService.createPointHistory(any(PointRequest.class), any(User.class)))
				.willReturn(res);

			// WHEN - THEN
			mockMvc.perform(post("/points/funds")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.price").value(1000))
				.andExpect(jsonPath("$.type").value("CHARGE"))
				.andExpect(jsonPath("$.status").value("PENDING"));
		}
	}

	@Nested
	@WithCustomMockUser
	@DisplayName("포인트 요청 조회 테스트")
	class GetMyPointRequestsTest {

		@Test
		@DisplayName("성공: 전체 포인트 요청 조회 (status 파라미터 없음)")
		void success_getMyPointRequests_withoutStatus() throws Exception {
			// Given
			List<PointHistoryResponse> responseList = List.of(
				new PointHistoryResponse(
					10000L,
					"123-456-789",
					PointHistoryType.CHARGE,
					PointHistoryStatus.PENDING,
					LocalDateTime.now()
				),
				new PointHistoryResponse(
					5000L,
					"987-654-321",
					PointHistoryType.CHARGE,
					PointHistoryStatus.PENDING,
					LocalDateTime.now()
				)
			);

			BDDMockito.given(pointHistoryService.findMyPointRequests(any(Long.class), isNull()))
				.willReturn(responseList);

			// When & Then
			mockMvc.perform(get("/points/requests"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].price").value(10000))
				.andExpect(jsonPath("$[0].account").value("123-456-789"))
				.andExpect(jsonPath("$[0].type").value("CHARGE"))
				.andExpect(jsonPath("$[0].status").value("PENDING"))
				.andExpect(jsonPath("$[1].price").value(5000))
				.andExpect(jsonPath("$[1].status").value("PENDING"));
		}

		@Nested
		@WithCustomMockUser
		@DisplayName("포인트 내역 목록 조회 테스트")
		class FindPointHistoriesTest {
			@Test
			@DisplayName("성공: 포인트 내역 목록 조회")
			void success_findPointHistories() throws Exception {
				// Given
				List<PointHistoryResponse> list = List.of(
					new PointHistoryResponse(
						1000L,
						"123-456-789",
						PointHistoryType.CHARGE,
						PointHistoryStatus.PENDING,
						LocalDateTime.now()
					),
					new PointHistoryResponse(
						500L,
						"987-654-321",
						PointHistoryType.CHARGE,
						PointHistoryStatus.PENDING,
						LocalDateTime.now()
					)
				);

				CommonPageResponse<PointHistoryResponse> pageResponse =
					CommonPageResponse.from(new PageImpl<>(list));

				BDDMockito.given(
						pointHistoryService.findMyPointHistories(any(Long.class), any(int.class), any(int.class)))
					.willReturn(pageResponse);

				// When & Then
				mockMvc.perform(get("/points/histories")
						.param("page", "1")
						.param("size", "10"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(2)))
					.andExpect(jsonPath("$.content[0].price").value(1000))
					.andExpect(jsonPath("$.content[0].type").value("CHARGE"))
					.andExpect(jsonPath("$.content[1].price").value(500))
					.andExpect(jsonPath("$.content[1].type").value("CHARGE"));
			}
		}
	}

	@Nested
	@WithCustomMockUser
	@DisplayName("포인트 요청 취소 테스트")
	class RejectPointRequestTest {

		@Test
		@DisplayName("성공: 포인트 요청 취소")
		void success_rejectPointRequest() throws Exception {
			// Given
			Long pointId = 1L;

			BDDMockito.willDoNothing()
				.given(pointHistoryService)
				.rejectPointRequest(any(Long.class), eq(pointId));

			// When & Then
			mockMvc.perform(delete("/points/points/{pointId}", pointId))
				.andExpect(status().isOk())
				.andExpect(content().string(""));

			// Verify
			verify(pointHistoryService, times(1))
				.rejectPointRequest(any(Long.class), eq(pointId));
		}

		@Test
		@DisplayName("성공: 다른 포인트 ID로 거절 요청")
		void success_rejectPointRequest_differentId() throws Exception {
			// Given
			Long pointId = 999L;

			BDDMockito.willDoNothing()
				.given(pointHistoryService)
				.rejectPointRequest(any(Long.class), eq(pointId));

			// When & Then
			mockMvc.perform(delete("/points/points/{pointId}", pointId))
				.andExpect(status().isOk())
				.andExpect(content().string(""));

			// Verify
			verify(pointHistoryService, times(1))
				.rejectPointRequest(any(Long.class), eq(pointId));
		}

		@Test
		@DisplayName("성공: 큰 숫자 포인트 ID로 거절 요청")
		void success_rejectPointRequest_largeId() throws Exception {
			// Given
			Long pointId = 9999999L;

			BDDMockito.willDoNothing()
				.given(pointHistoryService)
				.rejectPointRequest(any(Long.class), eq(pointId));

			// When & Then
			mockMvc.perform(delete("/points/points/{pointId}", pointId))
				.andExpect(status().isOk())
				.andExpect(content().string(""));

			// Verify service method called with correct parameters
			verify(pointHistoryService, times(1))
				.rejectPointRequest(any(Long.class), eq(pointId));
		}
	}
}
