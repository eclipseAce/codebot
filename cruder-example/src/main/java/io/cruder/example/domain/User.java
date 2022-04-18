package io.cruder.example.domain;

import io.cruder.apt.CompileMeta;
import io.cruder.apt.CompileScript;
import io.cruder.example.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@CompileScript("autocrud")
@Entity
public class User extends BaseEntity {

    @CompileMeta({"field(title: '用户名')", "forCreate('add', nonEmpty: true)"})
    private @Getter @Setter String username;

    @CompileMeta({"field(title: '密码')", "forCreate('add', nonEmpty: true)",
            "forUpdate('setPassword', nonEmpty: true)"})
    private @Getter @Setter String password;

    @CompileMeta({"field(title: '手机号')", "forCreate('add')", "forUpdate('setProfile')"})
    private @Getter @Setter String mobile;

    @CompileMeta({"field(title: '邮箱')", "forCreate('add')", "forUpdate('setProfile')"})
    private @Getter @Setter String email;

    @CompileMeta({"field(title: '是否锁定')", "forUpdate('setLocked')"})
    private @Getter @Setter boolean locked;

}
