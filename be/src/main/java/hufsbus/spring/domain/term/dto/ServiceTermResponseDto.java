package hufsbus.spring.domain.term.dto;

import hufsbus.spring.domain.term.entity.ServiceTerm;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ServiceTermResponseDto {

    private String name;
    private String contentType;

    public static ServiceTermResponseDto of(ServiceTerm serviceTerm) {

        ServiceTermResponseDto serviceTermResponseDto = new ServiceTermResponseDto();
        serviceTermResponseDto.name = serviceTerm.getName();
        serviceTermResponseDto.contentType = serviceTerm.getContentType();

        return serviceTermResponseDto;
    }
}
