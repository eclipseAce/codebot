package io.cruder.test.domain;

import io.cruder.test.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Entity
public class User extends BaseEntity {

    @Getter
    @Setter
    private String username;

    @Getter
    @Setter
    private String password;

    @Getter
    @Setter
    private String mobile;

    @Getter
    @Setter
    private String email;

    @Getter
    @Setter
    private boolean locked;

}
