package com.hust.commonlibrary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.Instant;

/**
 * Lớp cơ sở dành riêng cho các Entity SQL cần tính năng xóa mềm.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseSoftDeleteEntity<T extends Serializable> extends BaseEntity<T> implements SoftDeletable {

    private boolean isDeleted = false;
    private Instant deletedAt;
}
