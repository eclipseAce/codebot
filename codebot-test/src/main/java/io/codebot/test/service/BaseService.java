package io.codebot.test.service;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import io.codebot.test.core.QBaseEntity;

public abstract class BaseService {
    protected Predicate filterDeleted(Predicate predicate, QBaseEntity baseEntity) {
        return Expressions.allOf(baseEntity.deleted.isFalse(), Expressions.asBoolean(predicate));
    }
}
