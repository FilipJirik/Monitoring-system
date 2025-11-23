package cz.jirikfi.monitoringsystembackend.Services;

import java.security.SecureRandom;
import java.util.Base64;

public class GenerateApiKeyService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    public static String generate() {
        byte[] randomBytes = new byte[32]; // 256-bit key
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

}
