package com.hust.commonlibrary.aspect;

import com.hust.commonlibrary.annotation.CheckCourseOwner;
import com.hust.commonlibrary.constant.RedisPrefixConstants;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.commonlibrary.resolver.CourseIdResolver;
import com.hust.commonlibrary.service.RedisService;
import com.hust.commonlibrary.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
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
@ConditionalOnBean(RedisService.class)
public class CourseOwnerAspect {

    private final RedisService redisService;
    private final ApplicationContext applicationContext;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Before("@annotation(checkCourseOwner)")
    public void checkOwnership(JoinPoint joinPoint, CheckCourseOwner checkCourseOwner) {
        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        String courseId = null;

        // Parse context SpEL từ tham số đầu vào của method
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
            // KỊCH BẢN 1: Sử dụng courseId trực tiếp (Cho API Tạo mới)
            if (!checkCourseOwner.courseId().isBlank()) {
                Expression expression = parser.parseExpression(checkCourseOwner.courseId());
                courseId = expression.getValue(context, String.class);
            }
            // KỊCH BẢN 2: Sử dụng domainId + Resolver (Cho API Cập nhật/Xóa - Chống hack fake ID!)
            else if (!checkCourseOwner.domainId().isBlank() && !checkCourseOwner.resolver().isBlank()) {
                Expression idExpr = parser.parseExpression(checkCourseOwner.domainId());
                String domainId = idExpr.getValue(context, String.class);
                
                if (domainId != null && !domainId.isBlank()) {
                    // Dùng ApplicationContext để lôi Bean xịn bên Microservice kia ra query DB ngầm
                    CourseIdResolver resolverBean = applicationContext.getBean(checkCourseOwner.resolver(), CourseIdResolver.class);
                    courseId = resolverBean.resolveCourseId(domainId);
                }
            }
        } catch (Exception e) {
            log.error("Ultimate AOP Error: Lỗi phân tích cú pháp phân quyền", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // Nếu không tìm thấy courseId tương ứng, chặn truy cập cho an toàn
        if (courseId == null || courseId.isBlank()) {
            log.warn("Ultimate AOP Access Denied: Không phân giải được Course ID hợp lệ!");
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 🔒 ĐỐI CHIẾU BẢO MẬT TRÊN SHARED REDIS
        String key = RedisPrefixConstants.getSharedCourseOwnerKey(courseId);
        String ownerInstructorId = (String) redisService.get(key);

        if (ownerInstructorId == null || !ownerInstructorId.equals(currentUserId)) {
            log.warn("Ultimate AOP Access Denied: User {} cố gắng can thiệp trái phép khóa học {} của {}", 
                     currentUserId, courseId, ownerInstructorId);
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        log.info("Ultimate AOP Success: Đã cho phép truy cập hợp lệ cho User {} vào Course {}", currentUserId, courseId);
    }
}
