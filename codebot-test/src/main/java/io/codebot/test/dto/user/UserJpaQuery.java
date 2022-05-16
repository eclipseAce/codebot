package io.codebot.test.dto.user;

import io.codebot.test.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;

@Data
@Schema(description = "用户查询")
public class UserJpaQuery {

    @Schema(description = "用户名/手机号/邮箱关键字")
    private String keyword;

    @Schema(description = "创建时间范围")
    private LocalDateTime[] createdAt;

    public Predicate getKeywordSpec(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        if (StringUtils.isNotBlank(keyword)) {
            return cb.or(
                    cb.like(root.get("username"), "%" + keyword + "%"),
                    cb.like(root.get("mobile"), "%" + keyword + "%"),
                    cb.like(root.get("email"), "%" + keyword + "%")
            );
        }
        return null;
    }
}
