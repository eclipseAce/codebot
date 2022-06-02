package io.codebot.test;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.codebot.test.domain.QRole;
import io.codebot.test.domain.QUser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.util.List;

@EnableJpaAuditing
@EnableJpaRepositories
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @EventListener
    protected void test(ContextRefreshedEvent e) {
        List<Tuple> list = new JPAQuery<>(e.getApplicationContext().getBean(EntityManager.class))
                .select(QUser.user, QRole.role)
                .from(QUser.user)
                .leftJoin(QRole.role).on(QUser.user.roleId.eq(QRole.role.id))
                .fetch();
        System.out.println(list);
    }

}
