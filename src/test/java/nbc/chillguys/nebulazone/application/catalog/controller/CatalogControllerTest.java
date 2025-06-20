package nbc.chillguys.nebulazone.application.catalog.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import nbc.chillguys.nebulazone.application.catalog.dto.response.CatalogResponse;
import nbc.chillguys.nebulazone.application.catalog.dto.response.SearchCatalogResponse;
import nbc.chillguys.nebulazone.application.catalog.service.CatalogService;
import nbc.chillguys.nebulazone.application.review.dto.response.ReviewResponse;
import nbc.chillguys.nebulazone.config.TestSecurityConfig;
import nbc.chillguys.nebulazone.domain.catalog.entity.CatalogType;
import nbc.chillguys.nebulazone.support.MockMvc.TestMockConfig;

@Import({TestSecurityConfig.class, TestMockConfig.class})
@DisplayName("카탈로그 컨트롤러 단위 테스트")
@WebMvcTest(CatalogController.class)
class CatalogControllerTest {
	@MockitoBean
	private CatalogService catalogService;

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("카탈로그 검색 성공")
	void success_searchCatalog() throws Exception {
		// Given
		SearchCatalogResponse response = new SearchCatalogResponse(1L, "test", "desc",
			LocalDateTime.now(), CatalogType.GPU.name(), null, null, null, null);
		Page<SearchCatalogResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

		given(catalogService.searchCatalog(anyString(), any(), anyInt(), anyInt()))
			.willReturn(page);

		// When
		ResultActions perform = mockMvc.perform(get("/catalogs")
			.param("keyword", "test")
			.param("type", "GPU")
			.param("page", "1")
			.param("size", "10"));

		// Then
		perform.andDo(print())
			.andExpectAll(
				status().isOk(),
				jsonPath("$.content[0].catalogId")
					.value(1L),
				jsonPath("$.content[0].catalogName")
					.value("test"),
				jsonPath("$.content[0].catalogDescription")
					.value("desc"),
				jsonPath("$.totalElements")
					.value(1)
			);
	}

	@Test
	@DisplayName("카탈로그 조회 성공")
	void success_getCatalog() throws Exception {
		// Given
		Long catalogId = 1L;
		CatalogResponse response = new CatalogResponse(catalogId, "test", "desc",
			CatalogType.GPU.name(), LocalDateTime.now(), LocalDateTime.now(),
			List.of(new ReviewResponse(1L, "review", 1, LocalDateTime.now())));

		given(catalogService.getCatalog(catalogId))
			.willReturn(response);

		// When
		ResultActions perform = mockMvc.perform(get("/catalogs/{catalogId}", catalogId));

		// Then
		perform.andDo(print())
			.andExpectAll(
				status().isOk(),
				jsonPath("$.catalogId")
					.value(catalogId),
				jsonPath("$.catalogName")
					.value("test"),
				jsonPath("$.catalogDescription")
					.value("desc"),
				jsonPath("$.catalogType")
					.value("GPU"),
				jsonPath("$.reviews.[0].id")
					.value(1L),
				jsonPath("$.reviews.[0].content")
					.value("review"),
				jsonPath("$.reviews.[0].star")
					.value(1)
			);
	}

}
