package com.hust.orderservice.repository;


import com.hust.orderservice.constant.OrderStatus;
import com.hust.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserId(String userId);
    List<Order> findByUserIdAndStatus(String userId, OrderStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT i.courseId, COUNT(o) FROM Order o JOIN o.items i WHERE i.courseId IN :courseIds AND o.status = :status GROUP BY i.courseId")
    List<Object[]> countByCourseIdsAndStatus(@org.springframework.data.repository.query.Param("courseIds") List<String> courseIds, @org.springframework.data.repository.query.Param("status") OrderStatus status);

    boolean existsByUserIdAndStatusAndItems_CourseId(String userId, OrderStatus status, String courseId);
}
