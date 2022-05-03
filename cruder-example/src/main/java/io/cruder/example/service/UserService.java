package io.cruder.example.service;

import io.cruder.autoservice.annotation.AutoService;
import io.cruder.example.domain.User;
import io.cruder.example.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@AutoService(User.class)
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
