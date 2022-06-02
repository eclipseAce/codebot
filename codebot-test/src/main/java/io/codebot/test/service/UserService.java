package io.codebot.test.service;

import io.codebot.apt.EntityService;
import io.codebot.test.domain.User;
import io.codebot.test.dto.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@EntityService(User.class)
public abstract class UserService extends BaseService {
    @Autowired
    private PasswordEncoder passwordEncoder;

    protected void setPassword(User user, String password) {
        user.setPassword(passwordEncoder.encode(password));
    }

    public abstract long create(UserCreate dto);

    public abstract UserDetails createAndGet(UserCreate create);

    public abstract UserDetails updatePassword(long id, UserUpdatePassword dto);

    public abstract void update(long id, UserUpdate dto);

    public abstract UserDetails findById(long id);

    public abstract UserDetails findByUsername(String username);

    public abstract List<UserSummary> findList(UserQuery query);

    public abstract Page<UserSummary> findPage(UserQuery query, Pageable pageable);

    public abstract Page<UserSummary> findAllPage(Pageable pageable);
}

