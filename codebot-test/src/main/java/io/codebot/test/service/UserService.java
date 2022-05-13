package io.codebot.test.service;

import io.codebot.CrudService;
import io.codebot.test.domain.User;
import io.codebot.test.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@CrudService(User.class)
public interface UserService {

    long create(UserCreate dto);

    UserDetails createAndGet(UserCreate create);

    long createDefault();

    void updatePassword(UserSetPassword dto);

    UserDetails updatePassword(long id, UserSetPassword dto);

    void updateProfile(UserSetProfile dto);

    void updateLocked(UserSetLocked dto);

    UserDetails findById(long id);

    UserDetails findByUsername(String username);

    List<UserSummary> findList(UserJpaQuery query);

    Page<UserSummary> findPage(UserJpaQuery query, Pageable pageable);
}
