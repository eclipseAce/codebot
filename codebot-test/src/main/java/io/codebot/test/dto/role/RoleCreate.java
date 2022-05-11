package io.codebot.test.dto.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "角色新增")
public class RoleCreate {

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "权限")
    private Set<String> permissions;

}
