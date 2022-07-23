package org.avni_integration_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;


@Configuration
@EnableWebMvc
public class StaticResourceConfiguration implements WebMvcConfigurer {


    @Value("${static.path}")
    private String staticPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (staticPath != null) {
            registry
                    .addResourceHandler("/avni-int-admin-app/static/**")
                    .addResourceLocations("file:" + staticPath + "static/")
                    .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));
            registry
                    .addResourceHandler("/avni-int-admin-app/**")
                    .addResourceLocations("file:" + staticPath);
        }
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/avni-int-admin-app/")
                .setViewName("forward:/avni-int-admin-app/index.html");
    }

}