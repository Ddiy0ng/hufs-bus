package hufsbus.spring.domain.favorite.controller;

import hufsbus.spring.domain.favorite.dto.FavoriteCreateRequestDto;
import hufsbus.spring.domain.favorite.dto.FavoriteResponseDto;
import hufsbus.spring.domain.favorite.favoriteEnum.DayEnum;
import hufsbus.spring.domain.favorite.service.FavoriteService;
import hufsbus.spring.domain.timetable.timetableEnum.InOutCampusEnum;
import hufsbus.spring.global.dto.ApiResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/favorite")
    public ResponseEntity<ApiResponseDto<Void>> createFavorite(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid FavoriteCreateRequestDto favoriteCreateRequestDto) {

        // 인증 사용자 Principal로 부터 id 사용자 id를 가져올 것
        // 파라미터로 id도 줄 것
        favoriteService.createFavorite(favoriteCreateRequestDto, userId);

        return ResponseEntity.ok(ApiResponseDto.success("즐겨찾기에 등록했습니다."));
    }

    @PutMapping("/favorite")
    public ResponseEntity<ApiResponseDto<Void>> updateFavorite(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid FavoriteCreateRequestDto favoriteCreateRequestDto) {
        favoriteService.updateFavorite(favoriteCreateRequestDto, userId);

        return ResponseEntity.ok(
                ApiResponseDto.success("즐겨찾기 요일을 수정했습니다.")
        );
    }

    @DeleteMapping("favorite")
    public ResponseEntity<ApiResponseDto<Void>> deleteFavorite(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long favoriteId,
            @RequestParam DayEnum day) {

        // 인증 사용자 Principal로 부터 id 사용자 id를 가져올 것

        // 파라미터로 id도 줄 것
        favoriteService.removeFavorite(favoriteId, userId, day);

        return ResponseEntity.ok(ApiResponseDto.success("즐겨찾기에서 제거했습니다."));
    }

    @GetMapping("/favorite")
    public ResponseEntity<ApiResponseDto<List<FavoriteResponseDto>>> searchFavorites(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "IN_CAMPUS") InOutCampusEnum inOutCampus,
            @RequestParam(defaultValue = "MON") DayEnum day
    ) {

        // 인증 사용자 Principal로 부터 id 사용자 id를 가져올 것

        // 파라미터로 id도 줄 것
        List<FavoriteResponseDto> favoriteResponseDtoList = favoriteService.searchFavorites(inOutCampus, userId, day);

        return ResponseEntity.ok(ApiResponseDto.of(favoriteResponseDtoList, "즐겨찾기 조회에 성공했습니다."));
    }
}
