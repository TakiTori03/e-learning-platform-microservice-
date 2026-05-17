package com.hust.commonlibrary.aspect;

import com.hust.commonlibrary.annotation.DistributedLock;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(RedissonClient.class)
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String rawKey = distributedLock.key();
        String resolvedKey = null;

        // 1. Parse SpEL Context từ Method Arguments
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = signature.getParameterNames();

        EvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        try {
            // 2. Giải mã SpEL để sinh Lock Key cuối cùng
            Expression expression = parser.parseExpression(rawKey);
            resolvedKey = expression.getValue(context, String.class);
        } catch (Exception e) {
            log.error("DistributedLock Aspect SpEL Parsing Error", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        if (resolvedKey == null || resolvedKey.isBlank()) {
            log.error("DistributedLock Aspect Error: Không sinh được Lock Key từ SpEL '{}'", rawKey);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // Prefix chuẩn của hệ thống tránh đụng hàng key Redis khác
        String finalLockKey = "elearning:lock:" + resolvedKey;
        
        log.info("DistributedLock Attempting to acquire lock on: {}", finalLockKey);
        RLock lock = redissonClient.getLock(finalLockKey);
        
        boolean isLocked = false;
        try {
            // 3. Cố gắng lấy khóa toàn cục
            isLocked = lock.tryLock(
                    distributedLock.waitTime(), 
                    distributedLock.leaseTime(), 
                    distributedLock.timeUnit()
            );

            if (!isLocked) {
                // Thất bại -> Báo bận 429
                log.warn("DistributedLock Collision Detected: Không thể giữ khóa {} sau {}s", finalLockKey, distributedLock.waitTime());
                throw new AppException(ErrorCode.CONCURRENT_REQUEST);
            }

            log.info("DistributedLock SUCCESSFULLY ACQUIRED lock: {}", finalLockKey);
            
            // 4. Tiến hành thực thi Business Logic thực sự!
            return joinPoint.proceed();

        } finally {
            // 5. ĐẢM BẢO 100%: Chỉ mở khóa nếu luồng hiện tại đang nắm giữ khóa (chống crash hoặc nhầm luồng)
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("DistributedLock SUCCESSFULLY RELEASED lock: {}", finalLockKey);
            }
        }
    }
}
