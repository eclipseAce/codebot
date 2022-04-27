package io.cruder.example.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户密码修改")
public class UserSetLocked {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "是否锁定")
    private Boolean locked;

}
