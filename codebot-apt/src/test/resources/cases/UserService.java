package cases;

import cases.dto.*;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

public abstract class UserService {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Mapping(target = "password", qualifiedByName = "encodePassword")
    long create(UserCreate dto);

    UserDetails createAndGet(UserCreate create);

    long createDefault();

    void updatePassword(UserSetPassword dto);

    UserDetails updatePassword2(long id, UserSetPassword dto);

    void updateProfile(UserSetProfile dto);

    void updateLocked(UserSetLocked dto);

    UserDetails findById(long id);

    UserDetails findByUsername(String username);

    List<UserSummary> findList(UserQuery query);

    Page<UserSummary> findPage(UserQuery query, Pageable pageable);

    Page<UserSummary> findAllPage(Pageable pageable);
}

