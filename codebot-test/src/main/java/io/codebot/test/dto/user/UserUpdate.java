package io.codebot.test.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户资料修改")
public class UserUpdate {
    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "是否锁定")
    private Boolean locked;

    @Schema(description = "角色ID")
    private Long roleId;

}
