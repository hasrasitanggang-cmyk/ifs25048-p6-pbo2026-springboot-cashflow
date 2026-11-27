package org.delcom.ifs25048springbootcashflow.framework.config;

import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServlet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RobotsConfigTest {

    @Test
    void shouldRegisterRobotsServlet() throws Exception {

        RobotsConfig config = new RobotsConfig();

        ServletRegistrationBean<HttpServlet> registration =
            config.robotsServlet();

        assertNotNull(registration);
        assertNotNull(registration.getServlet());

        assertTrue(
            registration.getUrlMappings()
                .contains("/robots.txt")
        );

        assertEquals(
            "cashflowRobotsServlet",
            registration.getServletName()
        );
    }

    @Test
    void shouldReturnRobotsContent() throws Exception {

        RobotsConfig config = new RobotsConfig();

        Servlet servlet =
            config.robotsServlet().getServlet();

        MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/robots.txt");

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        servlet.service(request, response);

        assertEquals(200, response.getStatus());

        assertNotNull(response.getContentType());

        assertTrue(
            response.getContentType()
                .startsWith("text/plain")
        );

        assertEquals(
            "UTF-8",
            response.getCharacterEncoding()
        );

        byte[] expected = new ClassPathResource(
            "META-INF/resources/robots.txt"
        ).getContentAsByteArray();

        assertArrayEquals(
            expected,
            response.getContentAsByteArray()
        );

        String body = response.getContentAsString();

        assertTrue(body.contains("User-agent: *"));
        assertTrue(body.contains("Allow: /login"));
        assertTrue(body.contains("Allow: /register"));
        assertTrue(body.contains("Disallow: /profile"));
    }
}