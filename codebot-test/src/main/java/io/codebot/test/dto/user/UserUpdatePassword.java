package io.codebot.test.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户密码修改")
public class UserUpdatePassword {
    @Schema(description = "用户密码")
    private String password;
}
