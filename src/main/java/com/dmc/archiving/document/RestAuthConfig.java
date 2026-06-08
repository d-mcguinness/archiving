package com.dmc.archiving.document;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link RestAuthInterceptor} over the file/document REST endpoints.
 * Explicit include patterns (rather than /api/**) keep open endpoints such as
 * /api/auth/** unaffected.
 */
@Configuration
public class RestAuthConfig implements WebMvcConfigurer {

    private final RestAuthInterceptor restAuthInterceptor;

    public RestAuthConfig(RestAuthInterceptor restAuthInterceptor) {
        this.restAuthInterceptor = restAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(restAuthInterceptor)
                .addPathPatterns(
                        "/api/documents/**",   // DocumentController
                        "/api/upload", "/api/upload/**",  // FileUploadController raw uploads
                        "/api/download/**");   // FileUploadController raw download
    }
}
