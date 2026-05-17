package com.hust.identityservice.repository;

import com.hust.identityservice.entity.User;
import com.hust.identityservice.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import com.hust.identityservice.repository.projection.SignupProjection;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Boolean existsByEmail(String email);
    java.util.Optional<User> findByEmail(String email);
    List<User> findByStatus(UserStatus status);

    // Báo cáo User đăng ký mới theo tháng (Native Query PostgreSQL)
    @org.springframework.data.jpa.repository.Query(
        value = "SELECT TO_CHAR(created_at, 'YYYY-MM') as period, COUNT(id) as count " +
                "FROM users GROUP BY TO_CHAR(created_at, 'YYYY-MM') ORDER BY period", 
        nativeQuery = true
    )
    List<SignupProjection> getNewSignupsByMonth();

    // Lọc các User VIP hoặc Active tích cực nhất (sắp xếp theo last_login)
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.status = :status ORDER BY u.lastLogin DESC")
    org.springframework.data.domain.Page<User> getTopUsersByLastLogin(
        @org.springframework.data.repository.query.Param("status") UserStatus status, 
        org.springframework.data.domain.Pageable pageable
    );
}
