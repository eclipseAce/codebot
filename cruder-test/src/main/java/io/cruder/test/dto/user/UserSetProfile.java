package io.cruder.test.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户资料修改")
public class UserSetProfile {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

}
