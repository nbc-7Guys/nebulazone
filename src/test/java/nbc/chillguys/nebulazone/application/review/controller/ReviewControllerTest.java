package nbc.chillguys.nebulazone.application.review.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nbc.chillguys.nebulazone.application.review.dto.response.ReviewResponse;
import nbc.chillguys.nebulazone.application.review.service.ReviewService;
import nbc.chillguys.nebulazone.common.response.CommonPageResponse;
import nbc.chillguys.nebulazone.config.TestSecurityConfig;
import nbc.chillguys.nebulazone.support.MockMvc.TestMockConfig;

@WebMvcTest(ReviewController.class)
@Import({TestSecurityConfig.class, TestMockConfig.class})
@DisplayName("리뷰 컨트롤러 테스트")
class ReviewControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReviewService reviewService;

	@Test
	@DisplayName("리뷰 페이징 조회 성공")
	void success_findReviews() throws Exception {
		// given
		Long catalogId = 1L;
		int page = 1;
		int size = 10;

		List<ReviewResponse> reviews = List.of(
			new ReviewResponse(1L, "좋아요!", 5, LocalDateTime.now()),
			new ReviewResponse(2L, "별로예요", 2, LocalDateTime.now())
		);

		CommonPageResponse<ReviewResponse> response = CommonPageResponse.<ReviewResponse>builder()
			.content(reviews)
			.totalElements(2)
			.page(page)
			.size(size)
			.totalPages(1)
			.build();

		when(reviewService.findReviews(eq(catalogId), eq(page), eq(size))).thenReturn(response);

		// when & then
		mockMvc.perform(get("/catalogs/{catalogId}/reviews", catalogId)
				.param("page", String.valueOf(page))
				.param("size", String.valueOf(size)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].id").value(1))
			.andExpect(jsonPath("$.content[0].content").value("좋아요!"))
			.andExpect(jsonPath("$.content[0].star").value(5))
			.andExpect(jsonPath("$.content[1].id").value(2))
			.andExpect(jsonPath("$.content[1].star").value(2))
			.andExpect(jsonPath("$.page").value(page))
			.andExpect(jsonPath("$.size").value(size))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.totalPages").value(1));
	}

	@Test
	@DisplayName("존재하지 않는 카탈로그의 리뷰 조회 - 빈 페이지 반환")
	void success_findReviews_emptyResult() throws Exception {
		// given
		Long catalogId = 999L;
		int page = 1;
		int size = 10;

		CommonPageResponse<ReviewResponse> emptyResponse = CommonPageResponse.<ReviewResponse>builder()
			.content(List.of())
			.totalElements(0)
			.page(page)
			.size(size)
			.totalPages(0)
			.build();

		when(reviewService.findReviews(eq(catalogId), eq(page), eq(size))).thenReturn(emptyResponse);

		// when & then
		mockMvc.perform(get("/catalogs/{catalogId}/reviews", catalogId)
				.param("page", String.valueOf(page))
				.param("size", String.valueOf(size)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.content.length()").value(0))
			.andExpect(jsonPath("$.totalElements").value(0))
			.andExpect(jsonPath("$.page").value(page))
			.andExpect(jsonPath("$.size").value(size))
			.andExpect(jsonPath("$.totalPages").value(0));
	}
}
