package com.auctionflow.api.config;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ApiVersioningInterceptor implements HandlerInterceptor {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String DEFAULT_VERSION = "v1";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String apiVersion = request.getHeader(API_VERSION_HEADER);
        if (apiVersion == null || apiVersion.isEmpty()) {
            apiVersion = DEFAULT_VERSION;
        }

        // For now, only v1 is supported
        if (!"v1".equals(apiVersion)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Unsupported API version: " + apiVersion);
            return false;
        }

        // Store version in request attributes for later use
        request.setAttribute("apiVersion", apiVersion);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // Add version info to response headers
        String apiVersion = (String) request.getAttribute("apiVersion");
        if (apiVersion != null) {
            response.setHeader("X-API-Version", apiVersion);
        }
    }
}