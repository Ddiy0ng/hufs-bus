package hufsbus.spring.domain.timetable.service;

import hufsbus.spring.domain.timetable.dto.ExcelRequestDto;
import hufsbus.spring.domain.timetable.entity.BusRoute;
import hufsbus.spring.domain.timetable.entity.BusStop;
import hufsbus.spring.domain.timetable.entity.Timetable;
import hufsbus.spring.domain.timetable.repository.BusRouteRepository;
import hufsbus.spring.domain.timetable.repository.BusStopRepository;
import hufsbus.spring.domain.timetable.repository.TimetableRepository;
import hufsbus.spring.domain.timetable.timetableEnum.BusStopEnum;
import hufsbus.spring.domain.timetable.timetableEnum.BusWayEnum;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BusScheduleService {

    private final BusRouteRepository busRouteRepository;
    private final TimetableRepository timetableRepository;
    private final BusStopRepository busStopRepository;

    @Transactional
    public void createTimetables(MultipartFile multiPartFile) {

        try (InputStream inputStream = multiPartFile.getInputStream()) {
            List<ExcelRequestDto> excelRequestDtoList = FileUpload.fileToTimetable(inputStream);

            for (ExcelRequestDto excelRequestDto : excelRequestDtoList) {
                saveTimetable(excelRequestDto);
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.CREATE_TIMETABLE_EXCEPTION);
        }
    }

    // 단일 시간표 저장
    private void saveTimetable(ExcelRequestDto excelRequestDto) {

        LocalTime departAt = excelRequestDto.getDepartAt();
        BusWayEnum busWay = excelRequestDto.getBusWay();
        BusStopEnum startStop = excelRequestDto.getStartStop();
        List<BusStopEnum> route = parseRoute(excelRequestDto.getRoute());
        BusRoute busRoute = null;

        if (route.isEmpty())
            throw new CustomException(ErrorCode.PARSED_ROUTE_EMPTY_EXCEPTION);

        List<BusRoute> alreadyExistBusRoutes = busRouteRepository.findByBusWayAndStartStop(busWay, startStop);

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
            busRoute = BusRoute.of(busWay, startStop);
            busRouteRepository.save(busRoute);

            saveBusStop(route, busRoute);
        }

        if (timetableRepository.existsByBusRouteAndDepartAt(busRoute, departAt))
            return;

        // Timetable 객체 생성
        Timetable timetable = Timetable.of(departAt, busRoute);
        timetableRepository.save(timetable);
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
            busStopRepository.save(busStop);
        }
    }
}
