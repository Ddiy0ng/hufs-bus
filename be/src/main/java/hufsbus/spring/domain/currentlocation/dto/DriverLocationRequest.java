package hufsbus.spring.domain.currentlocation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DriverLocationRequest {
    private Long busId;
    private Double latitude;
    private Double longitude;
}
