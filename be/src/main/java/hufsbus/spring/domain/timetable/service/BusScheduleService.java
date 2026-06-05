package hufsbus.spring.domain.timetable.service;

import hufsbus.spring.domain.timetable.dto.BusRouteResponseDto;
import hufsbus.spring.domain.timetable.dto.ExcelRequestDto;
import hufsbus.spring.domain.timetable.dto.TimetableResponseDto;
import hufsbus.spring.domain.timetable.entity.BusRoute;
import hufsbus.spring.domain.timetable.entity.BusStop;
import hufsbus.spring.domain.timetable.entity.Timetable;
import hufsbus.spring.domain.stop.entity.StopCoordinate;
import hufsbus.spring.domain.stop.repository.StopCoordinateRepository;
import hufsbus.spring.domain.timetable.repository.BusRouteRepository;
import hufsbus.spring.domain.timetable.repository.BusStopRepository;
import hufsbus.spring.domain.timetable.repository.TimetableRepository;
import hufsbus.spring.domain.timetable.timetableEnum.BusStopEnum;
import hufsbus.spring.domain.timetable.timetableEnum.BusWayEnum;
import hufsbus.spring.domain.timetable.timetableEnum.InOutCampusEnum;
import hufsbus.spring.domain.util.FileUpload;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BusScheduleService {

    private final BusRouteRepository busRouteRepository;
    private final TimetableRepository timetableRepository;
    private final BusStopRepository busStopRepository;
    private final StopCoordinateRepository stopCoordinateRepository;
    private final FileUpload fileUpload;

    /*-----------액셀 업로드 및 저장-----------*/
    @Transactional
    public void createTimetables(MultipartFile multipartFile) {

        try (InputStream inputStream = multipartFile.getInputStream()) {
            List<ExcelRequestDto> excelRequestDtoList = fileUpload.fileToTimetable(inputStream);

            Map<String, Long> batchCounts = excelRequestDtoList.stream()
                    .collect(Collectors.groupingBy(
                            dto -> dto.getInOutCampus().name() + "|" + dto.getBusWay().name() + "|"
                                    + dto.getStartStop().name() + "|" + dto.getRoute().replaceAll("\\s+", "") + "|" + dto.getDepartAt(),
                            Collectors.counting()
                    ));

            Map<String, Long> savedInSession = new HashMap<>();

            for (ExcelRequestDto excelRequestDto : excelRequestDtoList) {
                saveTimetable(excelRequestDto, batchCounts, savedInSession);
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.PARSE_TIMETABLE_EXCEPTION);
        }
    }

    /*-----------시간표 변경-----------*/
    @Transactional
    public void updateTimetables(MultipartFile multipartFile) {

        try (InputStream inputStream = multipartFile.getInputStream()) {
            List<ExcelRequestDto> excelRequestDtoList = fileUpload.fileToTimetable(inputStream);

            // 기존 시간표 삭제
            timetableRepository.deleteAll();
            busStopRepository.deleteAll();
            busRouteRepository.deleteAll();

            Map<String, Long> batchCounts = excelRequestDtoList.stream()
                    .collect(Collectors.groupingBy(
                            dto -> dto.getInOutCampus().name() + "|" + dto.getBusWay().name() + "|"
                                    + dto.getStartStop().name() + "|" + dto.getRoute().replaceAll("\\s+", "") + "|" + dto.getDepartAt(),
                            Collectors.counting()
                    ));
            Map<String, Long> savedInSession = new HashMap<>();

            for (ExcelRequestDto excelRequestDto : excelRequestDtoList) {
                saveTimetable(excelRequestDto, batchCounts, savedInSession);
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.PARSE_TIMETABLE_EXCEPTION);
        }
    }

    // 단일 시간표 저장
    private void saveTimetable(ExcelRequestDto excelRequestDto, Map<String, Long> batchCounts, Map<String, Long> savedInSession) {

        LocalTime departAt = excelRequestDto.getDepartAt();
        InOutCampusEnum inOutCampus = excelRequestDto.getInOutCampus();
        BusWayEnum busWay = excelRequestDto.getBusWay();
        BusStopEnum startStop = excelRequestDto.getStartStop();
        List<BusStopEnum> route = parseRoute(excelRequestDto.getRoute());
        BusRoute busRoute = null;

        if (route.isEmpty())
            throw new CustomException(ErrorCode.PARSED_ROUTE_EMPTY_EXCEPTION);

        List<BusRoute> alreadyExistBusRoutes = busRouteRepository.findByInOutCampusAndBusWayAndStartStop(inOutCampus, busWay, startStop);

        for (BusRoute value : alreadyExistBusRoutes) {
            boolean isExistRoute = true;
            List<BusStop> alreadyExistsBusStops = busStopRepository.findByBusRouteOrderByStopOrderAsc(value);

            if (alreadyExistsBusStops.size() != route.size())
                continue;

            for (int i = 0; i < alreadyExistsBusStops.size(); i++) {
                BusStopEnum alreadyExistsBusStop = alreadyExistsBusStops.get(i).getBusStop();
                BusStopEnum checkBusStop = route.get(i);

                if (!alreadyExistsBusStop.equals(checkBusStop)) {
                    isExistRoute = false;
                    break;
                }
            }

            if (isExistRoute) {
                busRoute = value;
                break;
            }
        }

        if (busRoute == null) {
            busRoute = BusRoute.of(inOutCampus, busWay, startStop);
            busRouteRepository.save(busRoute);

            saveBusStop(route, busRoute);
        }

        String batchKey = inOutCampus.name() + "|" + busWay.name() + "|" + startStop.name() + "|"
                + excelRequestDto.getRoute().replaceAll("\\s+", "") + "|" + departAt;
        long expectedCount = batchCounts.getOrDefault(batchKey, 1L);
        long savedSoFar = savedInSession.getOrDefault(batchKey, 0L);

        if (savedSoFar >= expectedCount)
            return;

        // Timetable 객체 생성
        Timetable timetable = Timetable.of(departAt, busRoute);
        timetableRepository.save(timetable);
        savedInSession.merge(batchKey, 1L, Long::sum);
    }

    // 경로 파싱(여러 버스정류장들의 묶음이 하나로 들어오니까,,,)
    private List<BusStopEnum> parseRoute(String route) {

        try {
            List<BusStopEnum> stopList = Arrays.stream(route.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(BusStopEnum::from)
                    .toList();

            return stopList;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.ROUTE_PARSING_EXCEPTION);
        }
    }

    // 경로 BusStop에 저장
    private void saveBusStop(List<BusStopEnum> route, BusRoute busRoute) {
        for (int i = 0; i < route.size(); i++) {
            BusStop busStop = BusStop.of(route.get(i), i + 1, busRoute);
            List<StopCoordinate> coords = stopCoordinateRepository.findByStopName(route.get(i).getBusStopName());
            if (!coords.isEmpty()) {
                busStop.setCoordinates(coords.get(0).getLatitude(), coords.get(0).getLongitude());
            }
            busStopRepository.save(busStop);
        }
    }


    /*-----------시간표 조건 검색-----------*/
    @Transactional
    public List<BusRouteResponseDto> searchBusRoutes(String inOutCampusValue) {

        InOutCampusEnum inOutCampus = getDefaultOrSearchedInOutCampusValue(inOutCampusValue);

        List<BusRoute> busRouteList = busRouteRepository.findByInOutCampusOrderByIdAsc(inOutCampus);

        List<BusRouteResponseDto> busRouteResponseDtoList = new ArrayList<>();

        for (BusRoute value : busRouteList) {
            List<BusStop> busStopList = busStopRepository.findByBusRouteOrderByStopOrderAsc(value);
            busRouteResponseDtoList.add(BusRouteResponseDto.of(value, busStopList));
        }

        return busRouteResponseDtoList;
    }

    @Transactional
    public List<TimetableResponseDto> searchTimetables(String inOutCampusValue, Long routeIdValue, Integer startTimeValue) {

        InOutCampusEnum inOutCampus = getDefaultOrSearchedInOutCampusValue(inOutCampusValue);

        LocalTime startAt = null;
        LocalTime endAt = null;

        if  (startTimeValue != null) {

            startAt = LocalTime.of(startTimeValue, 0);
            endAt = startAt.plusHours(1);
        }

        List<Timetable> timetableList = timetableRepository.searchTimetables(inOutCampus, routeIdValue, startAt, endAt);

        List<TimetableResponseDto> timetableResponseDtoList = new ArrayList<>();

        for (Timetable value : timetableList) {
            List<BusStop> busStopList = busStopRepository.findByBusRouteOrderByStopOrderAsc(value.getBusRoute());
            timetableResponseDtoList.add(TimetableResponseDto.of(value, busStopList));
        }

        return timetableResponseDtoList;
    }

    // 조건 없으면 default로 교내
    private InOutCampusEnum getDefaultOrSearchedInOutCampusValue(String inOutCampus) {
        if (inOutCampus == null || inOutCampus.trim().isEmpty())
            return InOutCampusEnum.IN_CAMPUS;

        return InOutCampusEnum.from(inOutCampus);
    }
}
