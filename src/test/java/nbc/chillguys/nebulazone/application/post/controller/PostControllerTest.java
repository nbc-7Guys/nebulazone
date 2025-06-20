package nbc.chillguys.nebulazone.application.post.controller;

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

import nbc.chillguys.nebulazone.application.post.dto.request.CreatePostRequest;
import nbc.chillguys.nebulazone.application.post.dto.response.CreatePostResponse;
import nbc.chillguys.nebulazone.application.post.dto.response.GetPostResponse;
import nbc.chillguys.nebulazone.application.post.dto.response.SearchPostResponse;
import nbc.chillguys.nebulazone.application.post.service.PostService;
import nbc.chillguys.nebulazone.config.TestSecurityConfig;
import nbc.chillguys.nebulazone.domain.post.entity.PostType;
import nbc.chillguys.nebulazone.support.MockMvc.TestMockConfig;
import nbc.chillguys.nebulazone.support.mockuser.WithCustomMockUser;

@Import({TestSecurityConfig.class, TestMockConfig.class})
@DisplayName("게시글 컨트롤러 단위 테스트")
@WebMvcTest(PostController.class)
class PostControllerTest {
	@MockitoBean
	private PostService postService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Nested
	@DisplayName("게시글 생성")
	class CreatePostTest {

		@Test
		@DisplayName("게시글 생성 성공")
		@WithCustomMockUser
		void success_createPost() throws Exception {
			// given
			CreatePostRequest request = new CreatePostRequest("테스트 게시글", "테스트 본문", "free");

			LocalDateTime fixedTime = LocalDateTime.of(2025, 6, 13, 15, 30, 0);
			CreatePostResponse response = new CreatePostResponse(1L,
				"테스트 게시글",
				"테스트 게시글 본문",
				PostType.FREE,
				fixedTime,
				List.of("post_image.jpg"));

			given(postService.createPost(any(), any(CreatePostRequest.class), any())).willReturn(response);

			// when
			MockPart postPart = new MockPart("post", objectMapper.writeValueAsBytes(request));
			postPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);

			MockPart imagePart = new MockPart("images", "post_image.jpg", "test file content".getBytes());
			imagePart.getHeaders().setContentType(MediaType.IMAGE_JPEG);

			ResultActions perform = mockMvc.perform(multipart("/posts").part(postPart).part(imagePart));

			// then
			perform.andDo(print())
				.andExpectAll(status().isCreated(),
					jsonPath("$.postId").value(1L),
					jsonPath("$.title").value("테스트 게시글"),
					jsonPath("$.content").value("테스트 게시글 본문"),
					jsonPath("$.type").value(PostType.FREE.name()),
					jsonPath("$.modifiedAt").value("2025-06-13 15:30:00"),
					jsonPath("$.imageUrls").isArray(),
					jsonPath("$.imageUrls.length()").value(1)
				);
		}
	}

	@Nested
	@DisplayName("게시글 검색")
	class SearchPostTest {

		@Test
		@DisplayName("게시글 검색 성공")
		void success_searchPost() throws Exception {
			// Given
			SearchPostResponse response = new SearchPostResponse(1L, "title", "content", "FREE", 1L, "user",
				LocalDateTime.now(), List.of());
			Page<SearchPostResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

			given(postService.searchPost(anyString(), any(), anyInt(), anyInt())).willReturn(page);

			// When
			ResultActions perform = mockMvc.perform(
				get("/posts").param("keyword", "test").param("type", "FREE").param("page", "1").param("size", "10"));

			// Then
			perform.andDo(print())
				.andExpectAll(status().isOk(), jsonPath("$.content[0].postId").value(1L),
					jsonPath("$.content[0].title").value("title"),
					jsonPath("$.content[0].content").value("content"),
					jsonPath("$.content[0].type").value("FREE"),
					jsonPath("$.totalElements").value(1));
		}
	}

	@Nested
	@DisplayName("게시글 조회")
	class GetPostTest {

		@Test
		@DisplayName("게시글 조회 성공")
		void success_getPost() throws Exception {
			// Given
			Long postId = 1L;
			GetPostResponse response = new GetPostResponse(postId, "title", "content", "FREE", 1L, "user",
				LocalDateTime.now(), List.of());
			given(postService.getPost(postId)).willReturn(response);

			// When
			ResultActions perform = mockMvc.perform(get("/posts/{postId}", postId));

			// Then
			perform.andDo(print())
				.andExpectAll(status().isOk(),
					jsonPath("$.postId").value(postId),
					jsonPath("$.title").value("title"),
					jsonPath("$.content").value("content"));
		}
	}

}
