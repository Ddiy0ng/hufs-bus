package hufsbus.spring.domain.timetable.controller;

import hufsbus.spring.domain.timetable.service.BusScheduleService;
import hufsbus.spring.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class BusScheduleController {

    private final BusScheduleService busScheduleService;

    @PostMapping("/timetable")
    public ResponseEntity<ApiResponseDto<Void>> createTimetable(@RequestParam(value = "file") MultipartFile multipartFile) throws IOException {

        busScheduleService.createTimetables(multipartFile);

        return ResponseEntity.ok(ApiResponseDto.success("성공적으로 업로드했습니다."));
    }
}
