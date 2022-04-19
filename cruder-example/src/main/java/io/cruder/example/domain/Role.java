package io.cruder.example.domain;

import io.cruder.apt.CompileScript;
import io.cruder.example.core.BaseEntity;
import io.cruder.example.core.CRUD;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.HashSet;
import java.util.Set;

@CompileScript("autocrud")
@Entity
public class Role extends BaseEntity {

    @CRUD("field(title: '角色名') forCreate('add', nonEmpty: true)")
    private @Getter @Setter String name;

    @CRUD("field(title: '是否禁用') forUpdate('setDisabled', nonEmpty: true)")
    private @Getter @Setter boolean disabled;

    @CRUD("field(title: '权限') forCreate('add')")
    @ElementCollection
    private @Getter @Setter Set<String> permissions = new HashSet<>();
}
