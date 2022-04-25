package io.cruder.example.domain;

import io.cruder.apt.Codegen;
import io.cruder.example.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.HashSet;
import java.util.Set;

@Codegen("scripts/Crud")
@Entity
public class Role extends BaseEntity {

    private @Getter @Setter String name;

    private @Getter @Setter boolean disabled;

    @ElementCollection
    private @Getter @Setter Set<String> permissions = new HashSet<>();
}
