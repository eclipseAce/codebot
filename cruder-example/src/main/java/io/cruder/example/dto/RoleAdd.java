package io.cruder.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "角色新增")
public class RoleAdd {

    @Schema(description = "角色名称")
    private String username;

    @Schema(description = "权限")
    private Set<String> permissions;

}
