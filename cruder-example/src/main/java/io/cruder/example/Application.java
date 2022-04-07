package io.cruder.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing
@EnableJpaRepositories(excludeFilters = {
		@Filter(type = FilterType.REGEX, pattern = "io\\.cruder\\.example\\.template\\..*")
})
@ComponentScan(basePackages = "io.cruder.example", excludeFilters = {
		@Filter(type = FilterType.REGEX, pattern = "io\\.cruder\\.example\\.template\\..*")
})
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
