// security/filter/RequestSanitizationFilter.java

package com.LockSaveApplication.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RequestSanitizationFilter extends OncePerRequestFilter {

    // Patterns that indicate XSS or script injection attempts
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<(script|iframe|object|embed|form)[^>]*>.*?</(script|iframe|object|embed|form)>|" +
            "javascript:\\s*|" +
            "on\\w+\\s*=\\s*[\"'][^\"']*[\"']|" +
            "eval\\s*\\(|" +
            "expression\\s*\\(",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Wrap the request so we can sanitize parameters
        SanitizedRequestWrapper sanitized = new SanitizedRequestWrapper(request);
        filterChain.doFilter(sanitized, response);
    }

    // Skip sanitization for webhook endpoints —
    // raw payload must reach the controller unmodified for signature verification
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().contains("/webhooks/");
    }

    // ── Wrapper ───────────────────────────────────────────────────────────────

    private static class SanitizedRequestWrapper extends HttpServletRequestWrapper {

        // Own static logger — cannot access outer @Slf4j `log` from a static class
        private static final org.slf4j.Logger WRAPPER_LOG =
                org.slf4j.LoggerFactory.getLogger(SanitizedRequestWrapper.class);

        public SanitizedRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitize(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitize(values[i]);
            }
            return sanitized;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return sanitize(value);
        }

        private String sanitize(String value) {
            if (value == null) return null;
            String sanitized = XSS_PATTERN.matcher(value).replaceAll("");
            if (!sanitized.equals(value)) {
                WRAPPER_LOG.warn("XSS pattern detected and stripped from input");
            }
            return sanitized;
        }
    }
}