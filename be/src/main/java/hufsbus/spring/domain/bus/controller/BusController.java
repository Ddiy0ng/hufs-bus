package hufsbus.spring.domain.bus.controller;

import hufsbus.spring.domain.bus.dto.BusResponse;
import hufsbus.spring.domain.bus.dto.BusTagRequest;
import hufsbus.spring.domain.bus.dto.BusTagResponse;
import hufsbus.spring.domain.bus.service.BusService;
import hufsbus.spring.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/buses")
public class BusController {
    private final BusService busService;

    @GetMapping("/{timetableId}/seats")
    public ResponseEntity<ApiResponseDto<BusResponse>> getSeats(@PathVariable Long timetableId) {
        return ResponseEntity.ok(ApiResponseDto.of(busService.getSeats(timetableId), "여석 조회 성공"));
    }

    @PostMapping("/{timetableId}/tags")
    public ResponseEntity<ApiResponseDto<BusTagResponse>> tag(
            @PathVariable Long timetableId,
            @RequestBody BusTagRequest request) {
        return ResponseEntity.ok(ApiResponseDto.of(busService.tag(timetableId, request), "NFC 태그 처리 성공"));
    }
}
