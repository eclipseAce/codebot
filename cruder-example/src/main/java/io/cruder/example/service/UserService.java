package io.cruder.example.service;

import io.cruder.example.codegen.AutoService;
import io.cruder.example.domain.User;
import io.cruder.example.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@AutoService(User.class)
public interface UserService {

    long create(UserCreate dto);

    void updatePassword(UserSetPassword dto);

    void updateProfile(UserSetProfile dto);

    void updateLocked(UserSetLocked dto);

    UserDetails findById(long id);

    Page<UserSummary> findPage(UserQuery query, Pageable pageable);
}
