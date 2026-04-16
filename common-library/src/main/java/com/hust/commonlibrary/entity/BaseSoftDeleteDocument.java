package com.hust.commonlibrary.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/**
 * Lớp cơ sở dành riêng cho các Document cần tính năng xóa mềm.
 */
@Getter
@Setter
public abstract class BaseSoftDeleteDocument extends BaseDocument implements SoftDeletable {
    private boolean isDeleted = false;
    private Instant deletedAt;
}
