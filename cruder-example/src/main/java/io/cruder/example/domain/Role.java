package io.cruder.example.domain;

import io.cruder.example.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Role extends BaseEntity {

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private boolean disabled;

    @Getter
    @Setter
    @ElementCollection
    private Set<String> permissions = new HashSet<>();
}
