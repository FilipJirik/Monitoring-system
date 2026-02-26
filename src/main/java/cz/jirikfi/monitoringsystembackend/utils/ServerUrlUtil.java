package cz.jirikfi.monitoringsystembackend.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.InetAddress;

@Component
@Slf4j
public class ServerUrlUtil {

    private static final String SETUP_COMMAND_STRUCTURE = "./monitoring-agent setup --server-url=%s --device-id=%s --api-key=%s";
    private static final String DEFAULT_IP_ADDRESS = "127.0.0.1";

    @Value("${app.default-server-url:http://localhost:8080}")
    private String configuredServerUrl;

    private String resolveServerUrl() {
        try {
            var uriComponents = ServletUriComponentsBuilder.fromCurrentContextPath().build();

            String scheme = uriComponents.getScheme();  // "http" or "https"
            String host = uriComponents.getHost();      // eg. "localhost", "192.168.1.5"
            int port = uriComponents.getPort();         // eg. 8080, 80, 443

            if (host == null || host.equals("localhost") || host.equals(DEFAULT_IP_ADDRESS)) {
                host = InetAddress.getLocalHost().getHostAddress();
            }

            return scheme + "://" + host + ":" + port;

        } catch (Exception e) {
            log.warn("Failed to resolve dynamic URL, using fallback: {}", configuredServerUrl);
            return configuredServerUrl;
        }
    }

    public String getSetupCommand(String deviceId, String apiKey) {
        return String.format(
                SETUP_COMMAND_STRUCTURE,
                resolveServerUrl(), deviceId, apiKey
        );
    }
}
