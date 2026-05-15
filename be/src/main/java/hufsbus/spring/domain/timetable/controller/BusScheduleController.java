package hufsbus.spring.domain.timetable.controller;

import hufsbus.spring.domain.timetable.dto.BusRouteResponseDto;
import hufsbus.spring.domain.timetable.dto.TimetableResponseDto;
import hufsbus.spring.domain.timetable.service.BusScheduleService;
import hufsbus.spring.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class BusScheduleController {

    private final BusScheduleService busScheduleService;

    @PostMapping("/timetable")
    public ResponseEntity<ApiResponseDto<Void>> createTimetable(@RequestParam(value = "file") MultipartFile multipartFile) throws IOException {

        busScheduleService.createTimetables(multipartFile);

        return ResponseEntity.ok(ApiResponseDto.success("시간표를 성공적으로 업로드했습니다."));
    }

    @GetMapping("/inoutcampus")
    public ResponseEntity<ApiResponseDto<List<BusRouteResponseDto>>> searchBusRoutes(
            @RequestParam(required = false) String inOutCampus
    ) {
        List<BusRouteResponseDto> busRouteResponseDtoList =
                busScheduleService.searchBusRoutes(inOutCampus);

        return ResponseEntity.ok(ApiResponseDto.of(busRouteResponseDtoList, "경로 목록 조회에 성공했습니다."));
    }

    @GetMapping("/timetable")
    public ResponseEntity<ApiResponseDto<List<TimetableResponseDto>>> searchTimetables (
            @RequestParam(required = false) String inOutCampus,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Integer startTime
    ) {
        List<TimetableResponseDto> timetableResponseDtoList = busScheduleService.searchTimetables(inOutCampus, routeId, startTime);

        return ResponseEntity.ok(ApiResponseDto.of(timetableResponseDtoList, "시간표를 성공적으로 조회했습니다."));
    }
}
