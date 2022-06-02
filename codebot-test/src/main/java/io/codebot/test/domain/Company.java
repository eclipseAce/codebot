package io.codebot.test.domain;

import io.codebot.test.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Getter
@Setter
@Entity
public class Company extends BaseEntity {
    private String name;

    private String address;

    private String regionCode;
}
