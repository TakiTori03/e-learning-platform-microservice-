package com.hust.orderservice.repository;

import com.hust.orderservice.entity.OutboxEvent;
import com.hust.orderservice.constant.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")}) // -2 means SKIP LOCKED in Hibernate for PostgreSQL
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = :status")
    List<OutboxEvent> findByStatusForUpdate(OutboxStatus status);

    List<OutboxEvent> findByStatus(OutboxStatus status);
}

