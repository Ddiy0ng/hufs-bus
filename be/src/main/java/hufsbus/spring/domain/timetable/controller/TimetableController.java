package hufsbus.spring.domain.timetable.controller;

import hufsbus.spring.domain.route.entity.Route;
import hufsbus.spring.domain.timetable.dto.DepartResponse;
import hufsbus.spring.domain.timetable.dto.TimetableResponse;
import hufsbus.spring.domain.timetable.service.TimetableService;
import hufsbus.spring.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timetable")
public class TimetableController {
    private final TimetableService timetableService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<TimetableResponse>>> getTimetable(
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Route.RouteType routeType,
            @RequestParam int hour) {
        return ResponseEntity.ok(ApiResponseDto.of(
                timetableService.getTimetables(routeId, routeType, hour),
                "시간표 조회 성공"
        ));
    }

    @PatchMapping("/{timetableId}/depart")
    public ResponseEntity<ApiResponseDto<DepartResponse>> depart(@PathVariable Long timetableId) {
        return ResponseEntity.ok(ApiResponseDto.of(
                timetableService.depart(timetableId),
                "출발 처리 성공"
        ));
    }
}
