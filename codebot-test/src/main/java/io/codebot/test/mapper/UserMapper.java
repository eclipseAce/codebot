package io.codebot.test.mapper;

import io.codebot.test.domain.User;
import io.codebot.test.dto.user.UserCreate;
import io.codebot.test.dto.user.UserDetails;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User convertUserCreateToUser(UserCreate source);

    UserDetails convertUserToUserDetails(User source);
}
