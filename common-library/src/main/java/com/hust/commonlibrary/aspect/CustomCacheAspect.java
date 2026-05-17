package com.hust.commonlibrary.aspect;

import com.hust.commonlibrary.annotation.CustomCache;
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

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(RedisService.class)
public class CustomCacheAspect {

    private final RedisService redisService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    @Around("@annotation(customCache)")
    public Object handleCache(ProceedingJoinPoint joinPoint, CustomCache customCache) throws Throwable {
        String cacheKey = null;

        // 1. Trích xuất Cache Key qua SpEL
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

            Expression expression = expressionCache.computeIfAbsent(customCache.key(), parser::parseExpression);
            cacheKey = expression.getValue(context, String.class);
        } catch (Exception e) {
            log.error("CustomCache Aspect SpEL Parsing Error", e);
            // Lỗi parse SpEL -> Bỏ qua Cache, chạy DB gốc cho an toàn
            return joinPoint.proceed();
        }

        // Nếu SpEL parse ra null -> Bỏ qua Cache, chạy DB gốc
        if (cacheKey == null || cacheKey.isBlank()) {
            return joinPoint.proceed();
        }

        // 💡 KIÊN CỐ: Sử dụng trực tiếp key đã qua phân giải SpEL. 
        // Kết hợp với prefix Microservice của RedisService sẽ cho cấu trúc cực gọn gàng!
        String finalCacheKey = cacheKey;

        // 2. Thử đọc dữ liệu từ Cache (Bọc try-catch phòng vệ sập Redis)
        try {
            Object cachedValue = redisService.get(finalCacheKey);
            if (cachedValue != null) {
                log.debug("🚀 CustomCache HIT: key = {}", finalCacheKey);
                return cachedValue;
            }
        } catch (Exception e) {
            log.error("⚠️ Redis Connection Failure (Read) - Key: {}. Auto-switching to Database.", finalCacheKey, e);
            // Mạng/Redis sập -> Lướt qua lỗi, chạy DB luôn
            return joinPoint.proceed();
        }

        // 3. Cache MISS -> Thực thi nghiệp vụ gốc (Truy vấn Database)
        Object dbResult = joinPoint.proceed();

        // 4. Lưu kết quả vào Cache nếu thành công (Bọc try-catch phòng vệ)
        if (dbResult != null) {
            try {
                redisService.set(finalCacheKey, dbResult, customCache.ttl(), customCache.unit());
                log.debug("💾 CustomCache MISS -> STORED: key = {}, TTL = {} {}", 
                        finalCacheKey, customCache.ttl(), customCache.unit());
            } catch (Exception e) {
                log.error("⚠️ Redis Connection Failure (Write) - Key: {}", finalCacheKey, e);
                // Lỗi ghi cache cũng bỏ qua, không chặn luồng nghiệp vụ trả kết quả
            }
        }

        return dbResult;
    }
}
