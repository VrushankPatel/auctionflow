package com.auctionflow.api.config;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

public class DeprecationResponseInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, org.springframework.web.servlet.ModelAndView modelAndView) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            Class<?> controllerClass = handlerMethod.getBeanType();

            // Check method annotation
            ApiVersion methodAnnotation = method.getAnnotation(ApiVersion.class);
            if (methodAnnotation != null && methodAnnotation.deprecated()) {
                addDeprecationHeaders(response, methodAnnotation);
                return;
            }

            // Check class annotation
            ApiVersion classAnnotation = controllerClass.getAnnotation(ApiVersion.class);
            if (classAnnotation != null && classAnnotation.deprecated()) {
                addDeprecationHeaders(response, classAnnotation);
            }
        }
    }

    private void addDeprecationHeaders(HttpServletResponse response, ApiVersion annotation) {
        response.setHeader("Deprecation", "true");
        if (!annotation.deprecationMessage().isEmpty()) {
            response.setHeader("X-Deprecation-Message", annotation.deprecationMessage());
        }
        if (!annotation.sunsetDate().isEmpty()) {
            response.setHeader("Sunset", annotation.sunsetDate());
        }
        response.setHeader("Link", "</api/v2>; rel=\"successor-version\"");
    }
}