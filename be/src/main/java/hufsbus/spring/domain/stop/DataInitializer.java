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

            // ── 교내 셔틀버스 (상행) ──────────────────────────────────────
            StopCoordinate.builder().stopName("모현지석묘입구").latitude(37.335772).longitude(127.254110).build(),
            StopCoordinate.builder().stopName("기숙사사거리").latitude(37.336188).longitude(127.262313).build(),
            StopCoordinate.builder().stopName("도서관 앞").latitude(37.336813).longitude(127.266938).build(),
            StopCoordinate.builder().stopName("인문경상관 앞").latitude(37.339152).longitude(127.274092).build(),

            // ── 교내 셔틀버스 (하행) ──────────────────────────────────────
            StopCoordinate.builder().stopName("인문경상관 회차장 앞").latitude(37.339550).longitude(127.275500).build(),
            StopCoordinate.builder().stopName("교양관 앞").latitude(37.339257).longitude(127.273723).build(),
            StopCoordinate.builder().stopName("후생복지관 앞").latitude(37.337745).longitude(127.269356).build(),
            StopCoordinate.builder().stopName("공학관 앞").latitude(37.337074).longitude(127.267947).build(),
            StopCoordinate.builder().stopName("백년관 앞").latitude(37.336764).longitude(127.265806).build(),
            StopCoordinate.builder().stopName("국제사회교육원 앞").latitude(37.336108).longitude(127.261757).build(),
            StopCoordinate.builder().stopName("지석묘 앞").latitude(37.336065).longitude(127.254137).build(),

            // ── 교내 통학버스 하차장소 ────────────────────────────────────
            StopCoordinate.builder().stopName("외대 사거리 정류장").latitude(37.337253).longitude(127.250180).build(),
            StopCoordinate.builder().stopName("학생회관 앞").latitude(37.337455).longitude(127.269280).build(),
            StopCoordinate.builder().stopName("인문경상관 하차지점").latitude(37.339152).longitude(127.274092).build(),

            // ── 글로벌캠퍼스 (통학버스 공통 종점) ────────────────────────
            StopCoordinate.builder().stopName("글로벌캠퍼스").latitude(37.336804).longitude(127.265963).build(),

            // ── A. 판교, 서현 노선 ─────────────────────────────────────────
            StopCoordinate.builder().stopName("판교역 1번출구 판교역북편(07073)").latitude(37.396089).longitude(127.111397).build(),
            StopCoordinate.builder().stopName("성남역 1번출구 방면 백현마을3단지(07420)").latitude(37.391788).longitude(127.118136).build(),
            StopCoordinate.builder().stopName("서현역 5번출구 방면 이매촌한신(07616)").latitude(37.388030).longitude(127.124703).build(),

            // ── B. 경기광주역 노선 ────────────────────────────────────────
            StopCoordinate.builder().stopName("경기광주역 1번 출구 택시승차장").latitude(37.398995).longitude(127.251903).build(),
            StopCoordinate.builder().stopName("백년관 앞").latitude(37.336804).longitude(127.265963).build(),
            StopCoordinate.builder().stopName("인문경상관").latitude(37.339152).longitude(127.274092).build(),

            // ── 캠퍼스간 노선 ─────────────────────────────────────────────
            StopCoordinate.builder().stopName("글로벌캠퍼스 백년관").latitude(37.336804).longitude(127.265963).build(),
            StopCoordinate.builder().stopName("외대사거리 앞").latitude(37.337153).longitude(127.249848).build(),
            StopCoordinate.builder().stopName("서울캠퍼스").latitude(37.597298).longitude(127.058171).build(),

            // ── 1. 이문, 망우, 남양주 노선 ───────────────────────────────
            StopCoordinate.builder().stopName("서울캠퍼스 본관").latitude(37.597298).longitude(127.058171).build(),
            StopCoordinate.builder().stopName("망우역 건너편 명동칼국수").latitude(37.598427).longitude(127.094630).build(),
            StopCoordinate.builder().stopName("도농역 동화고 정문 앞").latitude(37.608029).longitude(127.161697).build(),
            StopCoordinate.builder().stopName("양정역 버스정류장(23278)").latitude(37.605680).longitude(127.192834).build(),
            StopCoordinate.builder().stopName("한국애니메이션고교건너편 버스정류장(28140)").latitude(37.533998).longitude(127.226088).build(),

            // ── 2. 이문, 돌곶이 노선 ─────────────────────────────────────
            StopCoordinate.builder().stopName("6호선 돌곶이역 5번출구 앞").latitude(37.611007).longitude(127.057721).build(),
            StopCoordinate.builder().stopName("6호선 석계역 4번출구(08147)").latitude(37.614779).longitude(127.066709).build(),
            StopCoordinate.builder().stopName("6,7호선 태릉입구역 7번출구(11117)").latitude(37.617864).longitude(127.076456).build(),

            // ── 3. 노원, 구리 노선 ────────────────────────────────────────
            StopCoordinate.builder().stopName("노원역 4번 출구 맞은편 KT노원지사 앞(11601)").latitude(37.655274).longitude(127.063762).build(),
            StopCoordinate.builder().stopName("구리 롯데백화점 맞은편 LG베스트샵 앞(22249)").latitude(37.601764).longitude(127.143389).build(),

            // ── 4-1. 이문, 구리 (하교) 노선 ──────────────────────────────
            StopCoordinate.builder().stopName("구리롯데백화점").latitude(37.602733).longitude(127.143840).build(),
            StopCoordinate.builder().stopName("태릉입구역").latitude(37.618483).longitude(127.075343).build(),
            StopCoordinate.builder().stopName("석계역").latitude(37.615184).longitude(127.065707).build(),
            StopCoordinate.builder().stopName("돌곶이역").latitude(37.610596).longitude(127.056452).build(),

            // ── 5. 광화문 노선 ────────────────────────────────────────────
            StopCoordinate.builder().stopName("광화문 1번출구").latitude(37.571967).longitude(126.974879).build(),
            StopCoordinate.builder().stopName("경복궁역 6번 출구").latitude(37.569765).longitude(126.976548).build(),

            // ── 6. 신길 노선 ──────────────────────────────────────────────
            StopCoordinate.builder().stopName("1호선 신길역 1,2번 출구(19225)").latitude(37.516728).longitude(126.916910).build(),

            // ── 7. 삼성, 수지 노선 ────────────────────────────────────────
            StopCoordinate.builder().stopName("삼성역 3번 출구 200m 전방 현대오토웨이 앞").latitude(37.506360).longitude(127.064227).build(),
            StopCoordinate.builder().stopName("대치동 은미아파트 강남나무병원 앞").latitude(37.499445).longitude(127.068009).build(),
            StopCoordinate.builder().stopName("수서역 1-1번 출구 서울의료원 셔틀버스정류장").latitude(37.487440).longitude(127.101078).build(),

            // ── 8. 잠실, 천호, 하남 노선 ─────────────────────────────────
            StopCoordinate.builder().stopName("잠실역 10번 출구 앞").latitude(37.514795).longitude(127.105335).build(),
            StopCoordinate.builder().stopName("천호역 6번 출구 7번 출구 사이").latitude(37.538243).longitude(127.124111).build(),
            StopCoordinate.builder().stopName("길동사거리, 강동세무서 버스정류장(25010)").latitude(37.534757).longitude(127.135976).build(),
            StopCoordinate.builder().stopName("길동주민센터 정류장(25008)").latitude(37.533995).longitude(127.142251).build(),
            StopCoordinate.builder().stopName("강동자이, 프라자아파트 정류장(25006)").latitude(37.536309).longitude(127.147884).build(),
            StopCoordinate.builder().stopName("상일 초등학교(25002)").latitude(37.545800).longitude(127.170731).build(),
            StopCoordinate.builder().stopName("황산사거리(28023)").latitude(37.549379).longitude(127.183925).build(),
            StopCoordinate.builder().stopName("하남시청역, 장지마을(28274)").latitude(37.540494).longitude(127.209302).build(),

            // ── 9. 일산 노선 ──────────────────────────────────────────────
            StopCoordinate.builder().stopName("마두역 4번 출구 앞(20665)").latitude(37.652406).longitude(126.777487).build(),
            StopCoordinate.builder().stopName("대곡역 중앙차로 서울방면(19007)").latitude(37.631463).longitude(126.809570).build(),
            StopCoordinate.builder().stopName("고양경찰서 앞 서울방면(19065)").latitude(37.628986).longitude(126.829293).build(),
            StopCoordinate.builder().stopName("디지털미디어시티역(중)(12007)").latitude(37.579104).longitude(126.900550).build(),

            // ── 10. 부평 노선 ─────────────────────────────────────────────
            StopCoordinate.builder().stopName("부평역 대아지하상가 입구 큰맘할매순대국 앞").latitude(37.490471).longitude(126.724432).build(),

            // ── 11. 범계 노선 ─────────────────────────────────────────────
            StopCoordinate.builder().stopName("범계역 1번 출구 하나증권 앞").latitude(37.391424).longitude(126.952616).build(),

            // ── 12. 안산 노선 ─────────────────────────────────────────────
            StopCoordinate.builder().stopName("중앙역 1번 출구 좌측 지하도입구 앞").latitude(37.316342).longitude(126.838904).build(),

            // ── 13. 범계, 안산 (하교) 노선 ───────────────────────────────
            StopCoordinate.builder().stopName("범계역").latitude(37.389853).longitude(126.950761).build(),
            StopCoordinate.builder().stopName("안산중앙역").latitude(37.316289).longitude(126.838909).build(),

            // ── 14. 수원 노선 ─────────────────────────────────────────────
            StopCoordinate.builder().stopName("수원역 9번출구 100m 앞 신한은행 건너편").latitude(37.267110).longitude(127.002678).build(),
            StopCoordinate.builder().stopName("장안공원 정류장(01411)").latitude(37.288257).longitude(127.012562).build(),
            StopCoordinate.builder().stopName("우만주공 4단지 앞 정류장(03088)").latitude(37.292207).longitude(127.030823).build(),

            // ── 15. 수지 노선 ─────────────────────────────────────────────
            StopCoordinate.builder().stopName("수지지역난방공사 앞 버스정류장(29782)").latitude(37.315226).longitude(127.088272).build(),
            StopCoordinate.builder().stopName("수지구청(29100)").latitude(37.321307).longitude(127.097831).build(),
            StopCoordinate.builder().stopName("풍덕천 삼거리버스 정류장 용인포은아트홀(29160)").latitude(37.324626).longitude(127.104781).build(),
            StopCoordinate.builder().stopName("한신아파트 버스정류장(29252)").latitude(37.327413).longitude(127.112706).build(),
            StopCoordinate.builder().stopName("동부아파트 버스정류장(29298)").latitude(37.329704).longitude(127.124042).build(),

            // ── 16. 신갈 노선 ─────────────────────────────────────────────
            StopCoordinate.builder().stopName("신갈굴다리 버스 정류장(29148)").latitude(37.270645).longitude(127.103139).build(),
            StopCoordinate.builder().stopName("상갈파출소 앞(29224)").latitude(37.271638).longitude(127.108938).build(),
            StopCoordinate.builder().stopName("기흥역(29269)").latitude(37.275720).longitude(127.115947).build(),
            StopCoordinate.builder().stopName("강남대역(29406)").latitude(37.270254).longitude(127.125991).build(),
            StopCoordinate.builder().stopName("쌍용아파트 앞(29347)").latitude(37.258760).longitude(127.142342).build(),
            StopCoordinate.builder().stopName("코업호텔 앞").latitude(37.238013).longitude(127.179560).build(),
            StopCoordinate.builder().stopName("명지대입구(29406)").latitude(37.235815).longitude(127.189974).build(),
            StopCoordinate.builder().stopName("용인터미널(29434)").latitude(37.233804).longitude(127.209275).build(),
            StopCoordinate.builder().stopName("용인중앙시장역(29430)").latitude(37.237222).longitude(127.208812).build(),
            StopCoordinate.builder().stopName("유림동 버스정류장(29452)").latitude(37.258633).longitude(127.212802).build()
        ));
    }
}
