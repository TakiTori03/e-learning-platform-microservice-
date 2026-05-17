package com.hust.commonlibrary.aspect;

import com.hust.commonlibrary.annotation.Idempotent;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.commonlibrary.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aspect Xử lý Chống trùng lặp yêu cầu Phân tán (Distributed Idempotency).
 * Sử dụng Redis Atomic SETNX kết hợp với Expression Caching đạt hiệu năng cực cao.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(RedisService.class)
public class IdempotentAspect {

    private final RedisService redisService;
    private final ExpressionParser parser = new SpelExpressionParser();
    
    // Hiệu năng cực đỉnh: Triệt tiêu hoàn toàn chi phí CPU parse SpEL lặp đi lặp lại
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String rawKey = null;

        // 1. Trích xuất Key lũy đẳng thông qua SpEL Cache
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Object[] args = joinPoint.getArgs();
            String[] parameterNames = signature.getParameterNames();

            EvaluationContext context = new StandardEvaluationContext();
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }

            Expression expression = expressionCache.computeIfAbsent(idempotent.key(), parser::parseExpression);
            rawKey = expression.getValue(context, String.class);
        } catch (Exception e) {
            log.error("Idempotent Aspect SpEL Parsing Error", e);
            // Gặp lỗi phân tích cú pháp -> Bỏ qua kiểm tra, cho phép chạy hàm gốc cho an toàn dữ liệu
            return joinPoint.proceed();
        }

        if (rawKey == null || rawKey.isBlank()) {
            return joinPoint.proceed();
        }

        // Tạo định danh ngăn trùng lặp cho hệ thống
        String idempotentKey = "idempotency:" + rawKey;

        // 2. Thao tác Khóa Nguyên tử (Atomic SETNX) lên Redis
        boolean isLocked = false;
        try {
            isLocked = redisService.setIfAbsent(
                    idempotentKey,
                    "LOCKED",
                    idempotent.expireTime(),
                    idempotent.unit()
            );
        } catch (Exception e) {
            log.error("⚠️ Redis Connection Error in IdempotentAspect - Key: {}. Bypassing lock checking.", idempotentKey, e);
            // Redis sập -> Lướt qua lỗi để nghiệp vụ chính tiếp tục (High Availability)
            return joinPoint.proceed();
        }

        // 3. Kiểm tra Trạng thái Khóa
        if (!isLocked) {
            log.warn("🛑 [Idempotency Triggered] Trùng lặp yêu cầu phát hiện. Khóa chặn: {}", idempotentKey);
            // Trả lỗi chuẩn hóa 429 (Too Many Requests) về phía máy khách
            throw new AppException(ErrorCode.CONCURRENT_REQUEST);
        }

        // 4. Thực thi logic nghiệp vụ
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            // Nếu tiến trình xảy ra lỗi trước khi kịp hoàn thành, cho phép xóa khóa chặn ngay
            // để người dùng có cơ hội Gửi Lại (Retry) lập tức mà không cần chờ hết TTL.
            try {
                redisService.delete(idempotentKey);
            } catch (Exception ignored) {}
            
            throw throwable;
        }
    }
}
