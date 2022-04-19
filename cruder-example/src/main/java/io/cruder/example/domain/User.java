package io.cruder.example.domain;

import io.cruder.apt.CompileScript;
import io.cruder.example.core.BaseEntity;
import io.cruder.example.core.CRUD;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@CompileScript("autocrud")
@CRUD("create('add', title: '新增用户')")
@CRUD("update('setPassword', title: '设置密码')")
@CRUD("update('setProfile', title: '设置用户资料')")
@CRUD("update('setLocked', title: '设置锁定状态')")
@CRUD("get('getDetails', title: '获取用户详情')")
@CRUD("query('query', title: '查询用户列表')")
@Entity
public class User extends BaseEntity {

    @CRUD("field(title: '用户名', nonEmpty: true).for('add,getDetails')")
    private @Getter @Setter String username;

    @CRUD("field(title: '密码', nonEmpty: true).for('add,setPassword')")
    private @Getter @Setter String password;

    @CRUD("field(title: '手机号').for('add,setProfile,getDetails')")
    private @Getter @Setter String mobile;

    @CRUD("field(title: '邮箱').for('add,setProfile,getDetails')")
    private @Getter @Setter String email;

    @CRUD("field(title: '是否锁定').for('setLocked,getDetails')")
    private @Getter @Setter boolean locked;

}
