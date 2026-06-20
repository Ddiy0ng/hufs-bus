package hufsbus.spring.domain.timetable.controller;

import hufsbus.spring.domain.timetable.dto.DepartResponse;
import hufsbus.spring.domain.timetable.service.TimetableService;
import hufsbus.spring.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timetable")
public class TimetableController {
    private final TimetableService timetableService;

    @PatchMapping("/{timetableId}/depart")
    public ResponseEntity<ApiResponseDto<DepartResponse>> depart(@PathVariable Long timetableId) {
        return ResponseEntity.ok(ApiResponseDto.of(
                timetableService.depart(timetableId),
                "출발 처리 성공"
        ));
    }

    @PatchMapping("/{timetableId}/finish")
    public ResponseEntity<ApiResponseDto<DepartResponse>> finish(@PathVariable Long timetableId) {
        return ResponseEntity.ok(ApiResponseDto.of(
                timetableService.finish(timetableId),
                "운행 종료 처리 성공"
        ));
    }
}
