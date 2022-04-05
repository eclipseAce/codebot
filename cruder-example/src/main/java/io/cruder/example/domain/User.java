package io.cruder.example.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class User implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private @Getter long id;

	private @Getter @Setter String username;

	private @Getter @Setter String password;
	
	private @Getter @Setter boolean locked;

	@CreatedDate
	private @Getter LocalDateTime createdAt;

	@LastModifiedDate
	private @Getter LocalDateTime updatedAt;
}
