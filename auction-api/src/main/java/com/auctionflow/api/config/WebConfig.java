package com.auctionflow.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve UI assets - order matters, more specific patterns first
        registry.addResourceHandler("/ui/assets/**")
                .addResourceLocations("classpath:/static/ui/assets/")
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver());
        
        // Serve UI directory with SPA fallback
        registry.addResourceHandler("/ui/**")
                .addResourceLocations("classpath:/static/ui/")
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        // If resource exists, return it
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        // Otherwise, fallback to index.html for SPA routing
                        return new ClassPathResource("/static/ui/index.html");
                    }
                });
        
        // Images directory
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(3600);
        
        // Other static resources
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
        
        registry.addResourceHandler("/graphiql/**")
                .addResourceLocations("classpath:/graphiql/")
                .setCachePeriod(3600);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect root to /ui
        registry.addViewController("/").setViewName("forward:/ui/index.html");
        // Serve index.html for /ui/
        registry.addViewController("/ui/").setViewName("forward:/ui/index.html");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiVersioningInterceptor())
                .addPathPatterns("/api/**"); // Only intercept API paths
        registry.addInterceptor(new DeprecationResponseInterceptor())
                .addPathPatterns("/api/**");
    }
}