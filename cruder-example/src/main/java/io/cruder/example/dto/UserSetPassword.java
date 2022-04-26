package io.cruder.example.dto;

import io.cruder.example.codegen.DtoAction;
import io.cruder.example.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@DtoAction(User.class)
@Schema(description = "用户密码修改")
public class UserSetPassword {
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户密码")
    private String password;
}
