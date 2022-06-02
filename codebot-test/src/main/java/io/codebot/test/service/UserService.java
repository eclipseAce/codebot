package io.codebot.test.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import io.codebot.apt.EntityService;
import io.codebot.test.core.BaseService;
import io.codebot.test.domain.*;
import io.codebot.test.dto.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.querydsl.QSort;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;

@EntityService(User.class)
public abstract class UserService extends BaseService {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    protected void setPassword(User user, String password) {
        user.setPassword(passwordEncoder.encode(password));
    }

    protected void setUsername(UserDetails userDetails, String username) {
        userDetails.setUsername(username);
    }

    protected Predicate filterDeleted(Predicate predicate) {
        return new BooleanBuilder(QUser.user.deleted.isFalse()).and(predicate);
    }

    public abstract long create(UserCreate dto);

    public abstract UserDetails createAndGet(UserCreate create);

    public abstract void updatePassword(long id, UserUpdatePassword dto);

    public abstract UserDetails update(long id, UserUpdate dto);

    public UserDetails findById(long id) {
        Tuple result = new JPAQuery<>(entityManager)
                .select(QUser.user, QRole.role)
                .from(QUser.user)
                .leftJoin(QRole.role).on(QUser.user.roleId.eq(QRole.role.id))
                .where(QUser.user.id.eq(id))
                .fetchOne();
        if (result == null) {
            return null;
        }
        UserDetails temp = new UserDetails();
        User user = result.get(QUser.user);
        if (user != null) {
            temp.setMobile(user.getMobile());
            temp.setEmail(user.getEmail());
            temp.setLocked(user.isLocked());
            temp.setRoleId(user.getRoleId());
            temp.setId(user.getId());
            temp.setCreatedAt(user.getCreatedAt());
            temp.setUpdatedAt(user.getUpdatedAt());
        }
        Role role = result.get(QRole.role);
        if (role != null) {
            temp.setRoleName(role.getName());
        }
        return temp;
    }

    public Page<UserDetails> findAll(Pageable pageable) {
        JPAQuery<Tuple> query = new JPAQuery<>(entityManager)
                .select(QUser.user, QRole.role, QCompany.company)
                .from(QUser.user)
                .leftJoin(QRole.role).on(QUser.user.roleId.eq(QRole.role.id))
                .leftJoin(QCompany.company).on(QUser.user.companyId.eq(QCompany.company.id));
        if (pageable.isPaged()) {
            query.offset(pageable.getOffset()).limit(pageable.getPageSize());
        }
        QueryResults<Tuple> result = query.fetchResults();
        List<UserDetails> list = result.getResults().stream().map(it -> {
            UserDetails temp = new UserDetails();
            User user = it.get(QUser.user);
            if (user != null) {
                temp.setMobile(user.getMobile());
                temp.setEmail(user.getEmail());
                temp.setLocked(user.isLocked());
                temp.setRoleId(user.getRoleId());
                temp.setCompanyId(user.getCompanyId());
                temp.setId(user.getId());
                temp.setCreatedAt(user.getCreatedAt());
                temp.setUpdatedAt(user.getUpdatedAt());
            }
            Role role = it.get(QRole.role);
            if (role != null) {
                temp.setRoleName(role.getName());
            }
            Company company = it.get(QCompany.company);
            if (company != null) {
                temp.setCompanyName(company.getName());
            }
            return temp;
        }).collect(Collectors.toList());
        return new PageImpl<>(list, pageable, result.getTotal());
    }

    public abstract UserDetails findByUsername(String username);

    public abstract List<UserSummary> findList(UserQuery query);

    public abstract Page<UserSummary> findPage(UserQuery query, Pageable pageable);

    public abstract Page<UserSummary> findAllPage(Pageable pageable);
}
