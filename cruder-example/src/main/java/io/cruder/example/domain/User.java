package io.cruder.example.domain;

import io.cruder.apt.Codegen;
import io.cruder.example.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Codegen("scripts/Crud")
@Entity
public class User extends BaseEntity {

    private @Getter @Setter String username;

    private @Getter @Setter String password;

    private @Getter @Setter String mobile;

    private @Getter @Setter String email;

    private @Getter @Setter boolean locked;

}
