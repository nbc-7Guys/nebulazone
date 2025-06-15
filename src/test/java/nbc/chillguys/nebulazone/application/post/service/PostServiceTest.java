package nbc.chillguys.nebulazone.application.post.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

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

import nbc.chillguys.nebulazone.application.post.dto.request.CreatePostRequest;
import nbc.chillguys.nebulazone.application.post.dto.response.CreatePostResponse;
import nbc.chillguys.nebulazone.application.post.dto.response.GetPostResponse;
import nbc.chillguys.nebulazone.application.post.dto.response.SearchPostResponse;
import nbc.chillguys.nebulazone.domain.auth.vo.AuthUser;
import nbc.chillguys.nebulazone.domain.post.dto.PostCreateCommand;
import nbc.chillguys.nebulazone.domain.post.dto.PostSearchCommand;
import nbc.chillguys.nebulazone.domain.post.entity.Post;
import nbc.chillguys.nebulazone.domain.post.entity.PostType;
import nbc.chillguys.nebulazone.domain.post.service.PostDomainService;
import nbc.chillguys.nebulazone.domain.post.vo.PostDocument;
import nbc.chillguys.nebulazone.domain.user.entity.Address;
import nbc.chillguys.nebulazone.domain.user.entity.OAuthType;
import nbc.chillguys.nebulazone.domain.user.entity.User;
import nbc.chillguys.nebulazone.domain.user.entity.UserRole;
import nbc.chillguys.nebulazone.domain.user.exception.UserErrorCode;
import nbc.chillguys.nebulazone.domain.user.exception.UserException;
import nbc.chillguys.nebulazone.domain.user.service.UserDomainService;
import nbc.chillguys.nebulazone.infra.aws.s3.S3Service;

@DisplayName("게시글 애플리케이션 서비스 단위 테스트")
@ExtendWith(MockitoExtension.class)
class PostServiceTest {
	@Mock
	private UserDomainService userDomainService;

	@Mock
	private PostDomainService postDomainService;

	@Mock
	private S3Service s3Service;

	@InjectMocks
	private PostService postService;

	private User user;
	private Post post;
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

		post = Post.builder()
			.title("테스트 제목1")
			.content("테스트 본문1")
			.type(PostType.FREE)
			.user(user)
			.build();
		ReflectionTestUtils.setField(post, "id", 1L);

		authUser = AuthUser.builder()
			.id(1L)
			.email("test@test.com")
			.roles(Set.of(UserRole.ROLE_USER))
			.build();
	}

	@Nested
	@DisplayName("게시글 검색 테스트")
	class SearchPostTest {
		@Test
		@DisplayName("게시글 검색 성공")
		void success_searchPost() {
			// Given
			PostSearchCommand command = PostSearchCommand.of("테스트", PostType.FREE, 1, 10);

			given(postDomainService.searchPost(command))
				.willReturn(new PageImpl<>(List.of(PostDocument.from(post)),
					PageRequest.of(0, 10), 1L));

			// When
			Page<SearchPostResponse> responses = postService.searchPost("테스트", PostType.FREE, 1, 10);

			// Then
			assertThat(responses.getContent()).hasSize(1);

			verify(postDomainService, times(1)).searchPost(command);

		}
	}

	@Nested
	@DisplayName("게시글 조회 테스트")
	class GetPostTest {
		@Test
		void success_getPost() {
			// Given
			Long postId = 1L;

			given(postDomainService.getActivePostWithUserAndImages(postId))
				.willReturn(post);

			// When
			GetPostResponse response = postService.getPost(postId);

			// Then
			assertThat(response.postId()).isEqualTo(postId);
			assertThat(response.title()).isEqualTo("테스트 제목1");
			assertThat(response.content()).isEqualTo("테스트 본문1");
			assertThat(response.type()).isEqualTo(PostType.FREE.name());

			verify(postDomainService, times(1)).getActivePostWithUserAndImages(postId);
		}
	}

	@Nested
	@DisplayName("게시글 생성 테스트")
	class CreatePostTest {

		@Test
		@DisplayName("게시글 생성 성공")
		void success_createPost() {
			// Given
			CreatePostRequest request = new CreatePostRequest(
				"테스트 게시글 제목",
				"테스트 게시글 내용",
				"free"
			);
			List<MultipartFile> files = List.of();

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willReturn(user);
			given(postDomainService.createPost(any(PostCreateCommand.class), any(List.class)))
				.willReturn(post);

			// When
			CreatePostResponse result = postService.createPost(authUser, request, files);

			// Then
			assertThat(result.postId()).isEqualTo(1L);
			assertThat(result.title()).isEqualTo("테스트 제목1");
			assertThat(result.content()).isEqualTo("테스트 본문1");
			assertThat(result.type()).isEqualTo(PostType.FREE);

			verify(userDomainService, times(1)).findActiveUserById(authUser.getId());
			verify(postDomainService, times(1)).createPost(any(PostCreateCommand.class), any(List.class));
			verify(postDomainService, times(1)).savePostToEs(post);
		}

		@Test
		@DisplayName("게시글 생성 성공 - 이미지 포함")
		void success_createPost_withImages() {
			// Given
			CreatePostRequest request = new CreatePostRequest(
				"테스트 게시글 제목",
				"테스트 게시글 내용",
				"free"
			);

			MultipartFile mockFile = mock(MultipartFile.class);
			List<MultipartFile> files = List.of(mockFile);
			String mockImageUrl = "https://test-image.jpg";

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willReturn(user);
			given(s3Service.generateUploadUrlAndUploadFile(mockFile))
				.willReturn(mockImageUrl);
			given(postDomainService.createPost(any(PostCreateCommand.class), any(List.class)))
				.willReturn(post);

			// When
			CreatePostResponse result = postService.createPost(authUser, request, files);

			// Then
			assertThat(result.postId()).isEqualTo(1L);
			assertThat(result.title()).isEqualTo("테스트 제목1");
			assertThat(result.content()).isEqualTo("테스트 본문1");
			assertThat(result.type()).isEqualTo(PostType.FREE);

			verify(userDomainService, times(1)).findActiveUserById(authUser.getId());
			verify(s3Service, times(1)).generateUploadUrlAndUploadFile(mockFile);
			verify(postDomainService, times(1)).createPost(any(PostCreateCommand.class), any(List.class));
		}

		@Test
		@DisplayName("게시글 생성 실패 - 존재하지 않는 사용자")
		void fail_createPost_userNotFound() {
			// Given
			CreatePostRequest request = new CreatePostRequest(
				"테스트 게시글 제목",
				"테스트 게시글 내용",
				"free"
			);
			List<MultipartFile> files = List.of();

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

			// When & Then
			assertThatThrownBy(() -> postService.createPost(authUser, request, files))
				.isInstanceOf(UserException.class)
				.hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);

			verify(userDomainService, times(1)).findActiveUserById(authUser.getId());
		}
	}

}
