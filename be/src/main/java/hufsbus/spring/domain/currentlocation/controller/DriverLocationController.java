package hufsbus.spring.domain.currentlocation.controller;

import hufsbus.spring.domain.currentlocation.dto.DriverLocationRequest;
import hufsbus.spring.domain.currentlocation.dto.DriverLocationResponse;
import hufsbus.spring.domain.currentlocation.service.DriverLocationService;
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
