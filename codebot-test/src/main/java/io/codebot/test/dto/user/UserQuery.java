package io.codebot.test.dto.user;

import io.codebot.test.domain.QRole;
import io.codebot.test.domain.QUser;
import io.codebot.test.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Schema(description = "用户查询")
public class UserQuery {

    @Schema(description = "用户名/手机号/邮箱关键字")
    private String keyword;

    @Schema(description = "角色ID")
    private Set<Long> roleIds;

    @Schema(description = "创建时间范围")
    private LocalDateTime[] createdAt;
}
