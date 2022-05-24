package io.codebot.test.service;

import io.codebot.AutoCrud;
import io.codebot.AutoExposed;
import io.codebot.test.domain.User;
import io.codebot.test.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@AutoCrud(User.class)
@AutoExposed("/api/user")
public interface UserService {

    /**
     * @swagger.title 创建用户
     * @param dto
     * @return
     */
    long create(UserCreate dto);

    /**
     * @swagger.title 创建用户
     * @param create
     * @return
     */
    UserDetails createAndGet(UserCreate create);

    /**
     * @swagger.title 创建默认用户
     * @return
     */
    long createDefault();

    /**
     * @swagger.title 修改用户密码
     * @param dto
     */
    void updatePassword(UserSetPassword dto);

    /**
     * @swagger.title 修改用户密码2
     * @param id
     * @param dto
     * @return
     */
    UserDetails updatePassword2(long id, UserSetPassword dto);

    /**
     * @swagger.title 修改用户资料
     * @param dto
     */
    void updateProfile(UserSetProfile dto);

    /**
     * @swagger.title 修改用户锁定状态
     * @param dto
     */
    void updateLocked(UserSetLocked dto);

    /**
     * @swagger.title 获取用户
     * @param id
     * @return
     */
    UserDetails findById(long id);

    /**
     * @swagger.title 根据用户名获取用户
     * @param username
     * @return
     */
    UserDetails findByUsername(String username);

    /**
     * @swagger.title 查询用户
     * @param query
     * @return
     */
    List<UserSummary> findList(UserQuery query);

    /**
     * @swagger.title 查询分页用户
     * @param query
     * @param pageable
     * @return
     */
    Page<UserSummary> findPage(UserQuery query, Pageable pageable);

    /**
     * @swagger.title 分页获取所有用户
     * @param pageable
     * @return
     */
    Page<UserSummary> findAllPage(Pageable pageable);
}
