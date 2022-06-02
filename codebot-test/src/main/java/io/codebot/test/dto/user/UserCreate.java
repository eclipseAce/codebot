package io.codebot.test.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;

@Data
@Schema(description = "用户新增")
public class UserCreate {

    @Size(min = 6, max = 24)
    @Schema(description = "用户名")
    private String username;

    @Size(min = 8, max = 32)
    @Schema(description = "密码")
    private String password;

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "角色ID")
    private Long roleId;

}
