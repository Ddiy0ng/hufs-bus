package hufsbus.spring.domain.term.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Table(name = "privacy_term")
@Entity
public class PrivacyTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "content_type")
    private String contentType;

    public static PrivacyTerm of(String name, String filePath, String contentType) {

        PrivacyTerm privacyTerm = new PrivacyTerm();
        privacyTerm.name = name;
        privacyTerm.filePath = filePath;
        privacyTerm.contentType = contentType;

        return privacyTerm;
    }
}
