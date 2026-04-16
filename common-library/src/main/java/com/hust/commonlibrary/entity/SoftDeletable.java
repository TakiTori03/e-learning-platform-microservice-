package com.hust.commonlibrary.entity;

import java.time.Instant;

/**
 * Interface hỗ trợ tính năng xóa mềm (Soft Delete) cho toàn bộ hệ thống.
 * Hỗ trợ cả SQL và NoSQL.
 */
public interface SoftDeletable {
    boolean isDeleted();
    void setDeleted(boolean deleted);
    
    Instant getDeletedAt();
    void setDeletedAt(Instant deletedAt);

    /**
     * Phương thức tiện ích để thực hiện xóa mềm nhanh
     */
    default void delete() {
        setDeleted(true);
        setDeletedAt(Instant.now());
    }

    /**
     * Cá phương thức tiện ích để khôi phục (Restore)
     */
    default void restore() {
        setDeleted(false);
        setDeletedAt(null);
    }
}
