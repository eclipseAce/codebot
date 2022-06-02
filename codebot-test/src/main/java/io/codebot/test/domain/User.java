package io.codebot.test.domain;

import io.codebot.test.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Getter
@Setter
@Entity
public class User extends BaseEntity {
    private String username;

    private String password;

    private String mobile;

    private String email;

    private boolean locked;

    private Long roleId;

    private Long companyId;
}
