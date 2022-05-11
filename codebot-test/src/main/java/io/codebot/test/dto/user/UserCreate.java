package io.codebot.test.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户新增")
public class UserCreate {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

}
