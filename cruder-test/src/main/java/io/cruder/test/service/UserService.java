package io.cruder.test.service;

import io.cruder.CrudService;
import io.cruder.test.domain.User;
import io.cruder.test.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    Page<UserSummary> findPage(UserQuery query, Pageable pageable);
}
