package io.codebot.test.domain;

import io.codebot.test.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
public class Role extends BaseEntity {
    private String name;

    private boolean disabled;

    @ElementCollection
    private Set<String> permissions = new HashSet<>();
}
