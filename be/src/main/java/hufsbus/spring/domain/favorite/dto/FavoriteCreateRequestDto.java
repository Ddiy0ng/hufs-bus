package hufsbus.spring.domain.favorite.dto;

import hufsbus.spring.domain.favorite.favoriteEnum.DayEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
public class FavoriteCreateRequestDto {

    @NotNull
    private Long timetableId;

    @NotNull
    private Set<DayEnum> days;
}
