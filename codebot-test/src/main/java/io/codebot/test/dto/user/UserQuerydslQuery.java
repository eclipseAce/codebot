package io.codebot.test.dto.user;

import com.querydsl.core.types.Predicate;
import io.codebot.test.domain.QUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户查询")
public class UserQuerydslQuery {

    @Schema(description = "用户名/手机号/邮箱关键字")
    private String keyword;

    @Schema(description = "创建时间范围")
    private LocalDateTime[] createdAt;

    public Predicate getKeywordSpec(QUser user) {
        return user.username.contains(keyword)
                .or(user.mobile.contains(keyword))
                .or(user.email.contains(keyword));
    }
}
