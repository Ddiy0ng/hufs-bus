package hufsbus.spring.domain.bus.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BusTagRequest {

    public enum TagType { BOARD, ALIGHT }

    private TagType type;
}
