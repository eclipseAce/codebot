package io.codebot.test.dto.user;

import com.google.common.collect.Lists;
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
import java.util.List;

@Data
@Schema(description = "用户查询")
public class UserQuery implements Specification<User> {

    @Schema(description = "用户名/手机号/邮箱关键字")
    private String keyword;

    @Schema(description = "创建时间范围")
    private LocalDateTime[] createdAt;

    @Override
    public Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        List<Predicate> where = Lists.newArrayList();
        if (StringUtils.isNotBlank(keyword)) {
            where.add(criteriaBuilder.or(
                    criteriaBuilder.like(root.get("username"), "%" + keyword + "%"),
                    criteriaBuilder.like(root.get("mobile"), "%" + keyword + "%"),
                    criteriaBuilder.like(root.get("email"), "%" + keyword + "%")
            ));
        }
        if (createdAt.length > 0 && createdAt[0] != null) {
            where.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdAt[0]));
        }
        if (createdAt.length > 1 && createdAt[1] != null) {
            where.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdAt[1]));
        }
        return criteriaBuilder.and(where.toArray(new Predicate[0]));
    }
}
