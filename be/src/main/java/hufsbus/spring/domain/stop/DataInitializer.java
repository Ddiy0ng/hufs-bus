package hufsbus.spring.domain.stop;

import hufsbus.spring.domain.stop.entity.StopCoordinate;
import hufsbus.spring.domain.stop.repository.StopCoordinateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final StopCoordinateRepository stopCoordinateRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (stopCoordinateRepository.count() > 0) return;

        stopCoordinateRepository.saveAll(List.of(
            // ── 교내 셔틀버스 ──────────────────────────────────────────────
            StopCoordinate.builder().stopName("지석묘").latitude(37.335772).longitude(127.254110).build(),
            StopCoordinate.builder().stopName("기숙사").latitude(37.336188).longitude(127.262313).build(),
            StopCoordinate.builder().stopName("도서관").latitude(37.336813).longitude(127.266938).build(),
            StopCoordinate.builder().stopName("어문관").latitude(37.339136).longitude(127.273767).build(),
            StopCoordinate.builder().stopName("인문경상관").latitude(37.339152).longitude(127.274092).build(),
            StopCoordinate.builder().stopName("교양관").latitude(37.339257).longitude(127.273723).build(),
            StopCoordinate.builder().stopName("공학관").latitude(37.337074).longitude(127.267947).build(),
            StopCoordinate.builder().stopName("백년관").latitude(37.336764).longitude(127.265806).build(),

            // ── 판교역 노선 ────────────────────────────────────────────────
            StopCoordinate.builder().stopName("판교역").latitude(37.396089).longitude(127.111397).build(),
            StopCoordinate.builder().stopName("성남역").latitude(37.391788).longitude(127.118136).build(),
            StopCoordinate.builder().stopName("서현역").latitude(37.388030).longitude(127.124703).build(),
            StopCoordinate.builder().stopName("한국외대 글로벌캠퍼스").latitude(37.336804).longitude(127.265963).build(),

            // ── 경기광주역 노선 ────────────────────────────────────────────
            StopCoordinate.builder().stopName("경기광주역 1번출구 택시 승강장").latitude(37.398995).longitude(127.251903).build()
        ));
    }
}
