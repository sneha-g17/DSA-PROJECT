package com.oneroute.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Local-demo boundary, not a substitute for production authentication or a reverse proxy. */
@Component
public class LocalRequestFilter extends OncePerRequestFilter {
    private static final int MAX_BODY = 16384;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Security-Policy", "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'none'; frame-ancestors 'none'");
        if (!allowedOrigin(request)) {
            reject(response, 403, "Local host and same-origin requests only");
            return;
        }
        if (!"POST".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        byte[] bytes = request.getInputStream().readNBytes(MAX_BODY + 1);
        if (bytes.length > MAX_BODY) {
            reject(response, 413, "Request body exceeds 16 KiB");
            return;
        }
        chain.doFilter(new BufferedBody(request, bytes), response);
    }

    private static boolean allowedOrigin(HttpServletRequest request) {
        if (!Set.of("localhost", "127.0.0.1").contains(request.getServerName())) {
            return false;
        }
        String origin = request.getHeader("Origin");
        return origin == null || origin.equals("http://localhost:" + request.getServerPort())
                || origin.equals("http://127.0.0.1:" + request.getServerPort());
    }

    private static void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private static final class BufferedBody extends HttpServletRequestWrapper {
        private final byte[] bytes;
        BufferedBody(HttpServletRequest request, byte[] bytes) {
            super(request);
            this.bytes = bytes;
        }
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Synchronous endpoint only");
                }
            };
        }
    }
}
