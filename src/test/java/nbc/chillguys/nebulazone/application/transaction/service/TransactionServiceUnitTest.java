package nbc.chillguys.nebulazone.application.transaction.service;

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

import nbc.chillguys.nebulazone.application.transaction.dto.response.FindDetailTransactionResponse;
import nbc.chillguys.nebulazone.application.transaction.dto.response.FindTransactionResponse;
import nbc.chillguys.nebulazone.common.response.CommonPageResponse;
import nbc.chillguys.nebulazone.domain.auth.vo.AuthUser;
import nbc.chillguys.nebulazone.domain.transaction.dto.TransactionFindAllInfo;
import nbc.chillguys.nebulazone.domain.transaction.dto.TransactionFindDetailInfo;
import nbc.chillguys.nebulazone.domain.transaction.entity.TransactionMethod;
import nbc.chillguys.nebulazone.domain.transaction.exception.TransactionErrorCode;
import nbc.chillguys.nebulazone.domain.transaction.exception.TransactionException;
import nbc.chillguys.nebulazone.domain.transaction.service.TransactionDomainService;
import nbc.chillguys.nebulazone.domain.user.entity.Address;
import nbc.chillguys.nebulazone.domain.user.entity.OAuthType;
import nbc.chillguys.nebulazone.domain.user.entity.User;
import nbc.chillguys.nebulazone.domain.user.entity.UserRole;
import nbc.chillguys.nebulazone.domain.user.exception.UserErrorCode;
import nbc.chillguys.nebulazone.domain.user.exception.UserException;
import nbc.chillguys.nebulazone.domain.user.service.UserDomainService;

@DisplayName("거래내역 애플리케이션 서비스 단위 테스트")
@ExtendWith(MockitoExtension.class)
class TransactionServiceUnitTest {

	@Mock
	private TransactionDomainService txDomainService;

	@Mock
	private UserDomainService userDomainService;

	@InjectMocks
	private TransactionService txService;

	private AuthUser authUser;
	private User user;
	private TransactionFindAllInfo transactionFindAllInfo;
	private TransactionFindDetailInfo transactionFindDetailInfo;

	@BeforeEach
	void init() {
		authUser = AuthUser.builder()
			.id(1L)
			.email("test@test.com")
			.roles(Set.of(UserRole.ROLE_USER))
			.build();

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
			.point(100000L)
			.oAuthType(OAuthType.DOMAIN)
			.roles(Set.of(UserRole.ROLE_USER))
			.addresses(addresses)
			.build();

		ReflectionTestUtils.setField(user, "id", 1L);

		transactionFindAllInfo = new TransactionFindAllInfo(
			1L,
			50000L,
			TransactionMethod.DIRECT,
			LocalDateTime.now(),
			"테스트 상품",
			true
		);

		transactionFindDetailInfo = new TransactionFindDetailInfo(
			1L,
			2L,
			"판매자닉네임",
			"seller@test.com",
			50000L,
			LocalDateTime.now(),
			TransactionMethod.DIRECT,
			1L,
			"테스트 상품",
			LocalDateTime.now().minusDays(1),
			true
		);
	}

	@Nested
	@DisplayName("거래내역 전체 조회")
	class FindMyTransactionsTest {

		@Test
		@DisplayName("내 거래내역 전체 조회 성공")
		void success_findMyTransactions() {
			// Given
			int page = 1;
			int size = 10;

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willReturn(user);

			Page<TransactionFindAllInfo> mockPage = new PageImpl<>(
				List.of(transactionFindAllInfo),
				PageRequest.of(0, 10),
				1L
			);

			given(txDomainService.findMyTransactions(user, page, size))
				.willReturn(mockPage);

			// When
			CommonPageResponse<FindTransactionResponse> result = txService.findMyTransactions(authUser, page, size);

			// Then
			assertThat(result.content()).hasSize(1);
			assertThat(result.totalElements()).isEqualTo(1L);
			assertThat(result.page()).isEqualTo(1);
			assertThat(result.size()).isEqualTo(10);

			FindTransactionResponse response = result.content().get(0);
			assertThat(response.txId()).isEqualTo(1L);
			assertThat(response.txPrice()).isEqualTo(50000L);
			assertThat(response.txMethod()).isEqualTo(TransactionMethod.DIRECT);
			assertThat(response.productName()).isEqualTo("테스트 상품");
			assertThat(response.isSold()).isTrue();

			verify(userDomainService, times(1)).findActiveUserById(authUser.getId());
			verify(txDomainService, times(1)).findMyTransactions(user, page, size);
		}

		@Test
		@DisplayName("내 거래내역 전체 조회 실패 - 존재하지 않는 사용자")
		void fail_findMyTransactions_userNotFound() {
			// Given
			int page = 1;
			int size = 10;

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

			// When & Then
			assertThatThrownBy(() -> txService.findMyTransactions(authUser, page, size))
				.isInstanceOf(UserException.class)
				.hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);

			verify(userDomainService, times(1)).findActiveUserById(authUser.getId());
			verify(txDomainService, never()).findMyTransactions(any(), anyInt(), anyInt());
		}
	}

	@Nested
	@DisplayName("거래내역 상세 조회")
	class FindMyTransactionTest {

		@Test
		@DisplayName("내 거래내역 상세 조회 성공")
		void success_findMyTransaction() {
			// Given
			Long transactionId = 1L;

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willReturn(user);

			given(txDomainService.findMyTransaction(user, transactionId))
				.willReturn(transactionFindDetailInfo);

			// When
			FindDetailTransactionResponse result = txService.findMyTransaction(authUser, transactionId);

			// Then
			assertThat(result.txId()).isEqualTo(1L);
			assertThat(result.sellerId()).isEqualTo(2L);
			assertThat(result.sellerNickname()).isEqualTo("판매자닉네임");
			assertThat(result.sellerEmail()).isEqualTo("seller@test.com");
			assertThat(result.txPrice()).isEqualTo(50000L);
			assertThat(result.txMethod()).isEqualTo(TransactionMethod.DIRECT);
			assertThat(result.productId()).isEqualTo(1L);
			assertThat(result.productName()).isEqualTo("테스트 상품");
			assertThat(result.isSold()).isTrue();

			verify(userDomainService, times(1)).findActiveUserById(authUser.getId());
			verify(txDomainService, times(1)).findMyTransaction(user, transactionId);
		}

		@Test
		@DisplayName("내 거래내역 상세 조회 실패 - 존재하지 않는 사용자")
		void fail_findMyTransaction_userNotFound() {
			// Given
			Long transactionId = 1L;

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

			// When & Then
			assertThatThrownBy(() -> txService.findMyTransaction(authUser, transactionId))
				.isInstanceOf(UserException.class)
				.hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);

			verify(userDomainService, times(1)).findActiveUserById(authUser.getId());
		}

		@Test
		@DisplayName("내 거래내역 상세 조회 실패 - 존재하지 않는 거래내역")
		void fail_findMyTransaction_txNotFound() {
			// Given
			Long transactionId = 999L;

			given(userDomainService.findActiveUserById(authUser.getId()))
				.willReturn(user);

			given(txDomainService.findMyTransaction(user, transactionId))
				.willThrow(new TransactionException(TransactionErrorCode.NOT_FOUNT_TRANSACTION));

			// When & Then
			assertThatThrownBy(() -> txService.findMyTransaction(authUser, transactionId))
				.isInstanceOf(TransactionException.class)
				.hasFieldOrPropertyWithValue("errorCode", TransactionErrorCode.NOT_FOUNT_TRANSACTION);

			verify(txDomainService, times(1)).findMyTransaction(user, transactionId);
		}

	}
}
