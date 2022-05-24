package io.codebot.test.service;

import io.codebot.apt.annotation.AutoCrud;
import io.codebot.apt.annotation.AutoExpose;
import io.codebot.apt.annotation.Exposed;
import io.codebot.test.domain.User;
import io.codebot.test.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


@AutoCrud(entity = User.class)
@AutoExpose(title = "用户接口", path = "/api/user")
public interface UserService {

    @Exposed(title = "创建用户")
    long create(@Exposed.Body UserCreate dto);

    @Exposed(title = "创建用户2")
    UserDetails createAndGet(@Exposed.Body UserCreate create);

    @Exposed(title = "创建默认用户")
    long createDefault();

    @Exposed(title = "修改用户密码")
    void updatePassword(@Exposed.Body UserSetPassword dto);

    @Exposed(title = "修改用户密码2")
    UserDetails updatePassword2(@Exposed.Path long id, @Exposed.Body UserSetPassword dto);

    @Exposed(title = "修改用户资料")
    void updateProfile(@Exposed.Body UserSetProfile dto);

    @Exposed(title = "修改用户锁定状态")
    void updateLocked(@Exposed.Body UserSetLocked dto);

    @Exposed(title = "获取用户")
    UserDetails findById(@Exposed.Path long id);

    @Exposed(title = "根据用户名获取用户")
    UserDetails findByUsername(@Exposed.Param String username);

    @Exposed(title = "查询用户")
    List<UserSummary> findList(@Exposed.Body UserQuery query);

    @Exposed(title = "查询分页用户")
    Page<UserSummary> findPage(@Exposed.Body UserQuery query, Pageable pageable);

    @Exposed(title = "分页获取所有用户")
    Page<UserSummary> findAllPage(Pageable pageable);
}
