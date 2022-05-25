package io.codebot.test.service;

import io.codebot.apt.annotation.Crud;
import io.codebot.apt.annotation.Expose;
import io.codebot.apt.annotation.Expose.Body;
import io.codebot.apt.annotation.Expose.Param;
import io.codebot.apt.annotation.Expose.Path;
import io.codebot.test.domain.User;
import io.codebot.test.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


@Crud(entity = User.class)
@Expose(title = "用户接口", path = "/api/user")
public interface UserService {

    @Expose(title = "创建用户")
    long create(@Body UserCreate dto);

    @Expose(title = "创建用户2")
    UserDetails createAndGet(@Body UserCreate create);

    @Expose(title = "创建默认用户")
    long createDefault();

    @Expose(title = "修改用户密码")
    void updatePassword(@Body UserSetPassword dto);

    @Expose(title = "修改用户密码2")
    UserDetails updatePassword2(@Path long id, @Body UserSetPassword dto);

    @Expose(title = "修改用户资料")
    void updateProfile(@Body UserSetProfile dto);

    @Expose(title = "修改用户锁定状态")
    void updateLocked(@Body UserSetLocked dto);

    @Expose(title = "获取用户")
    UserDetails findById(@Path long id);

    @Expose(title = "根据用户名获取用户")
    UserDetails findByUsername(@Param String username);

    @Expose(title = "查询用户")
    List<UserSummary> findList(@Body UserQuery query);

    @Expose(title = "查询分页用户")
    Page<UserSummary> findPage(@Body UserQuery query, Pageable pageable);

    @Expose(title = "分页获取所有用户")
    Page<UserSummary> findAllPage(Pageable pageable);
}
