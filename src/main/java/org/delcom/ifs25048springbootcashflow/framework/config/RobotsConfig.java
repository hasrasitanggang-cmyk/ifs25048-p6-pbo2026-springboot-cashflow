package org.delcom.ifs25048springbootcashflow.framework.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration(proxyBeanMethods = false)
public class RobotsConfig {

    @Bean
    public ServletRegistrationBean<HttpServlet> robotsServlet()
            throws IOException {

        byte[] content = new ClassPathResource(
            "META-INF/resources/robots.txt"
        ).getContentAsByteArray();

        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void doGet(
                HttpServletRequest request,
                HttpServletResponse response
            ) throws IOException {

                response.setContentType("text/plain");
                response.setCharacterEncoding("UTF-8");
                response.getOutputStream().write(content);
            }
        };

        ServletRegistrationBean<HttpServlet> registration =
            new ServletRegistrationBean<>(
                servlet, "/robots.txt"
            );

        registration.setName("cashflowRobotsServlet");

        return registration;
    }
}