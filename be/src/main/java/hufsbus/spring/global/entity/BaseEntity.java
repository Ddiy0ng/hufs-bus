package hufsbus.spring.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Getter
@MappedSuperclass   // 해당 클래스는 테이블로 만들지 않고 이 클래스를 상속한 Entity에게 필드 물려줌
@EntityListeners(AuditingEntityListener.class)  // 저장, 수정 이벤트를 감지해 생성일, 수정일 값 자동 삽입, 수정
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
