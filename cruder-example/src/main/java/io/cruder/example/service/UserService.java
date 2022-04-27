package io.cruder.example.service;

import io.cruder.example.codegen.AutoService;
import io.cruder.example.domain.User;
import io.cruder.example.dto.UserAdd;
import io.cruder.example.dto.UserSetLocked;
import io.cruder.example.dto.UserSetPassword;
import io.cruder.example.dto.UserSetProfile;

@AutoService(User.class)
public interface UserService {

    @AutoService.Creating
    long add(UserAdd dto);

    @AutoService.Updating
    void setPassword(UserSetPassword dto);

    @AutoService.Updating
    void setProfile(UserSetProfile dto);

    @AutoService.Updating
    void setLocked(UserSetLocked dto);

}
