package hufsbus.spring.domain.term.dto;

import hufsbus.spring.domain.term.entity.PrivacyTerm;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class PrivacyTermResponseDto {

    private String name;
    private String contentType;

    public static PrivacyTermResponseDto of(PrivacyTerm privacyTerm) {

        PrivacyTermResponseDto privacyTermResponseDto = new PrivacyTermResponseDto();
        privacyTermResponseDto.name = privacyTerm.getName();
        privacyTermResponseDto.contentType = privacyTerm.getContentType();

        return privacyTermResponseDto;
    }
}
