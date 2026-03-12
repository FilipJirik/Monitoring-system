package cz.jirikfi.monitoringsystembackend.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.exceptions.ApiErrorMessage;
import cz.jirikfi.monitoringsystembackend.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String BEARER_ = "Bearer ";
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    private static final UriTemplate METRICS_ENDPOINT_TEMPLATE = new UriTemplate("/api/devices/{deviceId}/metrics");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getMethod().equals("POST") &&
                METRICS_ENDPOINT_TEMPLATE.matches(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String jwt = getJwtFromRequest(request);

        if (jwt != null) {
            if (jwtUtil.isTokenValid(jwt)) {
                UUID userId = jwtUtil.getUserIdFromToken(jwt);
                String email = jwtUtil.getEmailFromToken(jwt);
                String username = jwtUtil.getUsernameFromToken(jwt);
                String roleName = jwtUtil.getRoleFromToken(jwt);

                Role role = (roleName != null) ? Role.valueOf(roleName) : Role.USER;

                UserPrincipal principalUser = UserPrincipal.builder()
                        .id(userId)
                        .email(email)
                        .username(username)
                        .role(role)
                        .password(null)
                        .build();

                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + role.name()));

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principalUser,
                        null,
                        authorities);

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                // Token is present but invalid
                sendError(response, request, HttpStatus.UNAUTHORIZED, "Unauthorized - Invalid or expired token");
                return; // Stop the filter chain
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response,
                           HttpServletRequest request,
                           HttpStatus status,
                           String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiErrorMessage error = new ApiErrorMessage(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_)) {
            return header.substring(BEARER_.length());
        }
        return null;
    }
}
