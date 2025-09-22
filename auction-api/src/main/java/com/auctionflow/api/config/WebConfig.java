package com.auctionflow.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // 1 hour cache for static assets
        registry.addResourceHandler("/graphiql/**")
                .addResourceLocations("classpath:/graphiql/")
                .setCachePeriod(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiVersioningInterceptor())
                .addPathPatterns("/api/**"); // Only intercept API paths
        registry.addInterceptor(new DeprecationResponseInterceptor())
                .addPathPatterns("/api/**");
    }
}