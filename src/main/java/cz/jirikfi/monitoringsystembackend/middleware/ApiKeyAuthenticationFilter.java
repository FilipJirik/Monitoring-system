package cz.jirikfi.monitoringsystembackend.middleware;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.jirikfi.monitoringsystembackend.entities.DevicePrincipal;
import cz.jirikfi.monitoringsystembackend.exceptions.ApiErrorMessage;
import cz.jirikfi.monitoringsystembackend.services.DeviceAuthService;
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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {


    private final DeviceAuthService deviceAuthService;
    private final ObjectMapper objectMapper;

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final UriTemplate METRICS_ENDPOINT_TEMPLATE = new UriTemplate("/api/devices/{deviceId}/metrics");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getMethod().equals("POST") ||
                !METRICS_ENDPOINT_TEMPLATE.matches(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String rawApiKey = request.getHeader(API_KEY_HEADER);

        if (rawApiKey == null || rawApiKey.isBlank()) {
            sendError(response, request, HttpStatus.UNAUTHORIZED, "Missing API Key");
            return;
        }

        try {
            Map<String, String> variables = METRICS_ENDPOINT_TEMPLATE.match(request.getRequestURI());
            UUID deviceIdFromUrl = UUID.fromString(variables.get("deviceId"));

            // Get device principal from cache
            DevicePrincipal principal = deviceAuthService.resolveDeviceByApiKey(rawApiKey, deviceIdFromUrl);

            if (principal != null && principal.getDeviceId().equals(deviceIdFromUrl)) {

                // throttle lastSeen using cache - call database only once per minute
                deviceAuthService.updateLastSeen(principal.getDeviceId());

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_DEVICE"))
                );// Dummy role for Spring Security

                SecurityContextHolder.getContext().setAuthentication(auth);
                filterChain.doFilter(request, response);

            } else {
                sendError(response, request, HttpStatus.UNAUTHORIZED, "Invalid API Key or Device ID");
            }

        } catch (IllegalArgumentException e) {
            sendError(response, request, HttpStatus.BAD_REQUEST, "Invalid Device UUID format");
        }
    }

    // unable to use Global Exception Handler - not instantiated yet
    private void sendError(HttpServletResponse response, HttpServletRequest request, HttpStatus status, String message) throws IOException {
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
}