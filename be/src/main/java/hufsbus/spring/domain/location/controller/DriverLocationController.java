package hufsbus.spring.domain.location.controller;

import hufsbus.spring.domain.location.dto.DriverLocationRequest;
import hufsbus.spring.domain.location.dto.DriverLocationResponse;
import hufsbus.spring.domain.location.service.DriverLocationService;
import hufsbus.spring.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driver")
public class DriverLocationController {

    private final DriverLocationService driverLocationService;

    @PostMapping("/location")
    public ResponseEntity<ApiResponseDto<DriverLocationResponse>> saveLocation(
            @RequestBody DriverLocationRequest request) {
        return ResponseEntity.ok(
                ApiResponseDto.of(
                        driverLocationService.saveLocation(request),
                        "위치 전송 성공"
                )
        );
    }

    @GetMapping("/location/{busId}")
    public ResponseEntity<ApiResponseDto<DriverLocationResponse>> getLocation(
            @PathVariable Long busId) {
        return ResponseEntity.ok(
                ApiResponseDto.of(
                        driverLocationService.getLatestLocation(busId),
                        "위치 조회 성공"
                )
        );
    }
}
