package hufsbus.spring.domain.timetable.controller;

import hufsbus.spring.domain.timetable.dto.LiveTimetableResponse;
import hufsbus.spring.domain.timetable.service.LiveTimetableService;
import hufsbus.spring.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timetables")
public class LiveTimetableController {

    private final LiveTimetableService liveTimetableService;

    @GetMapping("/{timetableId}/live")
    public ResponseEntity<ApiResponseDto<LiveTimetableResponse>> getLive(
            @PathVariable Long timetableId) {
        return ResponseEntity.ok(
                ApiResponseDto.of(
                        liveTimetableService.getLive(timetableId),
                        "실시간 조회 성공"
                )
        );
    }
}
