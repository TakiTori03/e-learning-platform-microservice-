package com.hust.commonlibrary.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Feign Interceptor: Tự động truyền JWT từ SecurityContext vào Header "Authorization" 
 * khi một Microservice gọi tới Microservice khác qua Feign Client.
 */
@Component
@Slf4j
@ConditionalOnClass({RequestInterceptor.class, JwtAuthenticationToken.class})
public class FeignClientInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_TOKEN_TYPE = "Bearer";

    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            String tokenValue = jwtAuthenticationToken.getToken().getTokenValue();
            
            log.debug("Feign Interceptor: Propagation JWT token to downstream service");
            template.header(AUTHORIZATION_HEADER, String.format("%s %s", BEARER_TOKEN_TYPE, tokenValue));
        } else {
            log.debug("Feign Interceptor: No JWT found in SecurityContext, skipping token propagation");
        }
    }
}
