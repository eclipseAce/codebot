package io.cruder.test.mapper;

import io.cruder.test.domain.User;
import io.cruder.test.dto.user.UserDetails;

public interface Test {
    UserDetails test(User user);
}
