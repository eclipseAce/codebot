package io.cruder.example.domain;

import io.cruder.apt.Template;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;
import java.time.LocalDateTime;

@Template("crud2.groovy")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Getter
    @Schema(description = "用户ID")
    private long id;

    @Getter
    @Setter
    @Schema(description = "用户名")
    private String username;

    @Getter
    @Setter
    @Schema(description = "密码")
    private String password;

    @Getter
    @Setter
    @Schema(description = "手机号码")
    private String mobile;

    @Getter
    @Setter
    @Schema(description = "EMail")
    private String email;

    @Getter
    @Setter
    @Schema(description = "是否锁定")
    private boolean locked;

    @CreatedDate
    @Getter
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Getter
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
