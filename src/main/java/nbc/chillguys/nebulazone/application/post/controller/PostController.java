package nbc.chillguys.nebulazone.application.post.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.application.post.dto.request.CreatePostRequest;
import nbc.chillguys.nebulazone.application.post.dto.request.UpdatePostRequest;
import nbc.chillguys.nebulazone.application.post.dto.response.CreatePostResponse;
import nbc.chillguys.nebulazone.application.post.dto.response.DeletePostResponse;
import nbc.chillguys.nebulazone.application.post.dto.response.GetPostResponse;
import nbc.chillguys.nebulazone.application.post.dto.response.SearchPostResponse;
import nbc.chillguys.nebulazone.application.post.dto.response.UpdatePostResponse;
import nbc.chillguys.nebulazone.application.post.service.PostService;
import nbc.chillguys.nebulazone.common.response.CommonPageResponse;
import nbc.chillguys.nebulazone.domain.common.validator.image.ImageFile;
import nbc.chillguys.nebulazone.domain.post.entity.PostType;
import nbc.chillguys.nebulazone.domain.user.entity.User;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@RequestBody(
		content = @Content(encoding = @Encoding(name = "post", contentType = MediaType.APPLICATION_JSON_VALUE))
	)
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CreatePostResponse> createPost(
		@AuthenticationPrincipal User user,
		@Valid @RequestPart("post") CreatePostRequest request,
		@RequestPart(value = "images", required = false) List<MultipartFile> multipartFiles) {

		CreatePostResponse postResponse = postService.createPost(user, request, multipartFiles);

		return ResponseEntity.status(HttpStatus.CREATED).body(postResponse);
	}

	@RequestBody(
		content = @Content(encoding = @Encoding(name = "post", contentType = MediaType.APPLICATION_JSON_VALUE))
	)
	@PutMapping(path = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<UpdatePostResponse> updatePost(
		@AuthenticationPrincipal User user,
		@PathVariable("postId") Long postId,
		@Valid @RequestPart("post") UpdatePostRequest request,
		@ImageFile @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles
	) {
		UpdatePostResponse res = postService.updatePost(user.getId(), postId, request, imageFiles);

		return ResponseEntity.ok(res);
	}

	@DeleteMapping("/{postId}")
	public ResponseEntity<DeletePostResponse> deletePost(
		@AuthenticationPrincipal User user,
		@PathVariable("postId") Long postId
	) {
		DeletePostResponse res = postService.deletePost(user.getId(), postId);

		return ResponseEntity.ok(res);
	}

	@GetMapping
	public ResponseEntity<CommonPageResponse<SearchPostResponse>> searchPost(
		@RequestParam(value = "keyword", required = false) String keyword,
		@RequestParam("type") PostType type,
		@RequestParam(value = "page", defaultValue = "1") Integer page,
		@RequestParam(value = "size", defaultValue = "10") Integer size
	) {
		Page<SearchPostResponse> responses = postService.searchPost(keyword, type, page, size);

		return ResponseEntity.ok(CommonPageResponse.from(responses));
	}

	@GetMapping("/{postId}")
	public ResponseEntity<GetPostResponse> getPost(@PathVariable("postId") Long postId) {
		GetPostResponse response = postService.getPost(postId);

		return ResponseEntity.ok(response);
	}

}
