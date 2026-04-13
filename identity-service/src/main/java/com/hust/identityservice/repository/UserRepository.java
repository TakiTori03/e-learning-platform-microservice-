package com.hust.identityservice.repository;

import com.hust.identityservice.entity.User;
import com.hust.identityservice.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Boolean existsByEmail(String email);
    java.util.Optional<User> findByEmail(String email);
    List<User> findByStatus(UserStatus status);
}
