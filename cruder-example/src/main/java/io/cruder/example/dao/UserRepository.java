package io.cruder.example.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.cruder.example.domain.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

}
