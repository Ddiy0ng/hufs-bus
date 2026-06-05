package hufsbus.spring.domain.term.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Table(name = "service_term")
@Entity
public class ServiceTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "content_type")
    private String contentType;

    public static ServiceTerm of(String name, String filePath, String contentType) {

        ServiceTerm serviceTerm = new ServiceTerm();
        serviceTerm.name = name;
        serviceTerm.filePath = filePath;
        serviceTerm.contentType = contentType;

        return serviceTerm;
    }
}
