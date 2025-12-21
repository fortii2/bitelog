package me.forty2.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import me.forty2.dto.UserDTO;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JwtUtils {

    public static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * header . payload . signature
     * all of 3 need to encode to base64url without "=", like:
     * jwt = base64url(header).base64url(payload).signature
     * signature = base64url(hs256/rs256(base64url(header).base64url(payload)))
     */
    public static String generateJWT(UserDTO userDTO, String secret) throws Exception {
        String header = objectMapper.writeValueAsString(new JwtHeader());
        String payload = objectMapper.writeValueAsString(new JwtPayload(userDTO, System.currentTimeMillis() / 1000 + 60 * 60));

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        String toSign = encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8)) +
                CommonConstants.DOT +
                encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        String signature = encoder.encodeToString(hmacSha256(toSign, secret));

        return toSign + CommonConstants.DOT + signature;
    }

    private static byte[] hmacSha256(String data, String secret) throws Exception {
        Mac sha256HMAC = Mac.getInstance(CommonConstants.HS256);

        SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                CommonConstants.HS256
        );

        sha256HMAC.init(secretKey);

        return sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    @Getter
    private static class JwtHeader {
        public final String alg = CommonConstants.JWT_ALG;
        public final String typ = CommonConstants.JWT_TYP;

    }

    @Getter
    private static class JwtPayload {
        public UserDTO userDTO;
        public long iat;
        public long exp;

        public JwtPayload(UserDTO userDTO, long exp) {
            this.userDTO = userDTO;
            this.iat = System.currentTimeMillis() / 1000;
            this.exp = exp;
        }
    }
}
