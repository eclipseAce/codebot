package io.cruder.test.service;

import io.cruder.CrudService;
import io.cruder.test.domain.User;
import io.cruder.test.dto.user.*;
import io.cruder.test.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@CrudService(entity = User.class, repository = UserRepository.class)
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

    Page<UserSummary> findPage(UserQuery query, Pageable pageable);
}
