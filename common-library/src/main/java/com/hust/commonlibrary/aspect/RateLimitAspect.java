package com.hust.commonlibrary.aspect;

import com.hust.commonlibrary.annotation.RateLimit;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.commonlibrary.service.RedisService;
import com.hust.commonlibrary.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(RedisService.class)
public class RateLimitAspect {

    private final RedisService redisService;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Before("@annotation(rateLimit)")
    public void checkRateLimit(JoinPoint joinPoint, RateLimit rateLimit) {
        String targetKey = resolveRateLimitKey(joinPoint, rateLimit);
        String finalKey = "elearning:ratelimit:" + targetKey;

        // Thực hiện tăng nguyên tử và tự động thiết lập TTL bằng Lua Script để chống rò rỉ khóa vĩnh viễn
        Long currentCount;
        try {
            currentCount = redisService.incrementAndExpire(finalKey, rateLimit.period());
        } catch (Exception e) {
            log.error("⚠️ Redis Connection Error in RateLimitAspect on key: {}. Bypassing rate limit check.", finalKey, e);
            // Redis sập -> Cho phép request đi qua để đảm bảo tính sẵn sàng (High Availability)
            return;
        }

        // Kiểm tra vượt ngưỡng
        if (currentCount != null && currentCount > rateLimit.limit()) {
            log.warn("RateLimit Triggered on key: {}. Limit: {}/{}s. Current: {}", 
                     finalKey, rateLimit.limit(), rateLimit.period(), currentCount);
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
        }

        log.debug("RateLimit Tracked on key: {}. Count: {}/{}", finalKey, currentCount, rateLimit.limit());
    }

    private String resolveRateLimitKey(JoinPoint joinPoint, RateLimit rateLimit) {
        // ƯU TIÊN 1: Giải mã SpEL nếu được cung cấp
        if (!rateLimit.key().isBlank()) {
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

                Expression expression = parser.parseExpression(rateLimit.key());
                String key = expression.getValue(context, String.class);
                if (key != null && !key.isBlank()) {
                    return "custom:" + key;
                }
            } catch (Exception e) {
                log.error("RateLimit Aspect SpEL Parsing Error", e);
            }
        }

        // ƯU TIÊN 2: Lấy User ID từ SecurityContext nếu có
        try {
            String userId = SecurityUtils.getCurrentUserIdOrThrow();
            if (userId != null && !userId.isBlank()) {
                return "user:" + userId;
            }
        } catch (Exception e) {
            // Bỏ qua nếu user chưa đăng nhập
        }

        // ƯU TIÊN 3: Tự động lấy địa chỉ Client IP làm chìa khóa dự phòng
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ip = getClientIp(request);
            return "ip:" + ip;
        }

        return "global:default";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Trường hợp chạy qua Load Balancer, lấy IP đầu tiên
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
