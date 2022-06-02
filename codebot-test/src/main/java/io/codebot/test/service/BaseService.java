package io.codebot.test.service;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import io.codebot.test.core.QBaseEntity;

public abstract class BaseService {
    protected Predicate filterDeleted(Predicate predicate) {
        return Expressions.allOf(
                QBaseEntity.baseEntity.deleted.isFalse(),
                Expressions.asBoolean(predicate)
        );
    }
}
