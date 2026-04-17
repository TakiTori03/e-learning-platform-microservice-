package com.hust.orderservice.constant;

public enum OrderStatus {
    PENDING,    // Đang chờ thanh toán
    SUCCESS,    // Thanh toán thành công, đã cấp quyền sở học khóa học
    CANCELLED,  // Người dùng chủ động hủy hoặc thanh toán thất bại
    EXPIRED,    // Hết hạn thanh toán (thường sau 15-30p link VNPay hết hiệu lực)
    REFUNDED    // Đã hoàn tiền cho khách hàng
}
