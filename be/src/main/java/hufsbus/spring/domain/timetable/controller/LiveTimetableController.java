package hufsbus.spring.domain.timetable.controller;

import hufsbus.spring.domain.timetable.dto.LiveTimetableResponse;
import hufsbus.spring.domain.timetable.service.LiveTimetableService;
import hufsbus.spring.domain.timetable.service.SseEmitterService;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timetables")
public class LiveTimetableController {

    private final LiveTimetableService liveTimetableService;
    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/{timetableId}/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long timetableId) {
        SseEmitter emitter = sseEmitterService.subscribe(timetableId);

        try {
            LiveTimetableResponse snapshot = liveTimetableService.getLive(timetableId);
            emitter.send(SseEmitter.event().name("snapshot").data(snapshot));
        } catch (CustomException e) {
            if (e.getErrorCode() != ErrorCode.LOCATION_NOT_FOUND) {
                emitter.completeWithError(e);
            }
            // LOCATION_NOT_FOUND: 운행 전 상태 - snapshot 없이 연결만 유지
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
