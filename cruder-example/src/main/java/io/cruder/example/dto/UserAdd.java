package io.cruder.example.dto;

import io.cruder.example.codegen.CreatingDTO;
import io.cruder.example.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@CreatingDTO(User.class)
@Schema(description = "用户新增")
public class UserAdd {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

}
