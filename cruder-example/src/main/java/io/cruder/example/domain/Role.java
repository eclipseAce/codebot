package io.cruder.example.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import io.cruder.apt.Template;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;

@Template("crud.groovy")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Role implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private @Getter long id;

	private @Getter @Setter String name;

	private @Getter @Setter boolean disabled;

	@ElementCollection
	private @Getter @Setter Set<String> permissions = new HashSet<>();

	@CreatedDate
	private @Getter LocalDateTime createdAt;

	@LastModifiedDate
	private @Getter LocalDateTime updatedAt;
}
