package io.codebot.test.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import io.codebot.apt.EntityService;
import io.codebot.test.core.BaseService;
import io.codebot.test.domain.QUser;
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

    protected void setUsername(UserDetails userDetails, String username) {
        userDetails.setUsername(username);
    }

    protected void setUsername(UserSummary userSummary, String username) {
        userSummary.setUsername(username);
    }

    protected Predicate filterDeleted(Predicate predicate) {
        return new BooleanBuilder(QUser.user.deleted.isFalse()).and(predicate);
    }

    public abstract long create(UserCreate dto);

    public abstract UserDetails createAndGet(UserCreate create);

    public abstract void updatePassword(long id, UserUpdatePassword dto);

    public abstract UserDetails update(long id, UserUpdate dto);

    public abstract UserDetails findById(long id);

    public abstract UserDetails findByUsername(String username);

    public abstract List<UserSummary> findList(UserQuery query);

    public abstract Page<UserSummary> findPage(UserQuery query, Pageable pageable);

    public abstract Page<UserSummary> findAllPage(Pageable pageable);
}
