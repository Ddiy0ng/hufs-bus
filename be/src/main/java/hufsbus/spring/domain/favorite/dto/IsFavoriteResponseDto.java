package hufsbus.spring.domain.favorite.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class IsFavoriteResponseDto {

    private Long favoriteId;
    private boolean isFavorite;

    public static IsFavoriteResponseDto of(Long favoriteId, boolean isFavorite) {

        IsFavoriteResponseDto isFavoriteResponseDto = new IsFavoriteResponseDto();
        isFavoriteResponseDto.favoriteId = favoriteId;
        isFavoriteResponseDto.isFavorite = isFavorite;

        return isFavoriteResponseDto;
    }
}
