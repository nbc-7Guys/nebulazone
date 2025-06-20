package nbc.chillguys.nebulazone.application.auction.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nebulazone.domain.auction.entity.Auction;
import nbc.chillguys.nebulazone.domain.auction.exception.AuctionErrorCode;
import nbc.chillguys.nebulazone.domain.auction.exception.AuctionException;
import nbc.chillguys.nebulazone.domain.auction.service.AuctionDomainService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionSchedulerService {

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(30);
	private final Map<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

	private final AuctionDomainService auctionDomainService;
	private final AutoAuctionService autoAuctionService;

	/**
	 * 경매 자동 종료 스케줄러 등록
	 * @param auction 등록할 경매
	 * @param productId 상품 id
	 * @author 전나겸
	 */
	public void autoAuctionEndSchedule(Auction auction, Long productId) {
		long seconds = Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();

		if (seconds <= 0) {
			throw new AuctionException(AuctionErrorCode.AUCTION_END_TIME_INVALID);
		}

		ScheduledFuture<?> future = scheduler.schedule(() -> {

			autoAuctionService.autoEndAuctionAndCreateTransaction(auction.getId(), productId);

			tasks.remove(auction.getId());

		}, seconds, TimeUnit.SECONDS);

		tasks.put(auction.getId(), future);
		log.info("자동 낙찰 스케줄러 등록 완료. auctionId: {}, {} 초 후 실행, 등록된 스케줄러 수: {}", auction.getId(), seconds, tasks.size());
	}

	/**
	 * 경매 스케줄러 취소
	 * @param auctionId 취소할 경매 아이디
	 * @author 전나겸
	 */
	public void cancelSchedule(Long auctionId) {
		ScheduledFuture<?> future = tasks.remove(auctionId);
		if (future != null) {
			future.cancel(true);
		}
	}

	/**
	 * 서버 재시작 시 날라간 삭제 및 종료 되지 않은 경매의 스케줄러를 복구<br>
	 * Auction 조회 시 상품과 상품 판매자 정보도 함께 조회 함
	 * @author 전나겸
	 */
	@PostConstruct
	public void recoverSchedules() {
		log.info("서버 재시작 - 경매 스케줄 복구 시작");
		List<Auction> auctionList = auctionDomainService.findActiveAuctionsWithProductAndSeller();

		auctionList.stream()
			.filter(auction -> !auction.isWon())
			.filter(auction -> Duration.between(LocalDateTime.now(), auction.getEndTime()).isPositive())
			.forEach(auction -> autoAuctionEndSchedule(auction, auction.getProductId()));
	}

	/**
	 * 서버 종료 시 스레드 종료
	 * @author 전나겸
	 */
	@PreDestroy
	public void shutdown() {
		log.info("서버 종료 - 경매 스케줄러 스레드 종료");
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				log.warn("스케줄러가 정상 종료 되지 않음, 강제 종료 수행");
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			log.warn("스케줄러 종료 중 에러 발생, 강제 종료 수행");
			scheduler.shutdownNow();
		}
	}
}
