package me.forty2.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.forty2.dto.UserDTO;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

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

    public static boolean checkJWT(String jwt, String secret) throws Exception {
        String[] splits = jwt.split("\\.");
        if (splits.length != 3) {
            return false;
        }

        Base64.Decoder urlDecoder = Base64.getUrlDecoder();

        byte[] decodedHeader = urlDecoder.decode(splits[0].getBytes(StandardCharsets.UTF_8));
        JwtHeader jwtHeader = objectMapper.readValue(decodedHeader, JwtHeader.class);
        if (!Objects.equals(jwtHeader.getAlg(), CommonConstants.JWT_ALG) ||
                !Objects.equals(jwtHeader.getTyp(), CommonConstants.JWT_TYP)) {
            return false;
        }

        byte[] decodedPayload = urlDecoder.decode(splits[1].getBytes(StandardCharsets.UTF_8));
        JwtPayload jwtPayload = objectMapper.readValue(decodedPayload, JwtPayload.class);
        if (jwtPayload.getExp() < System.currentTimeMillis() / 1000) {
            return false;
        }

        String signAgain = Base64.getUrlEncoder().withoutPadding().encodeToString(hmacSha256(splits[0] + CommonConstants.DOT + splits[1], secret));
        if (!signAgain.equals(splits[2])) {
            return false;
        }

        return true;
    }

    public static UserDTO getPayload(String jwt, String secret) throws Exception {
        if (!checkJWT(jwt, secret)) {
            return null;
        }

        String[] splits = jwt.split("\\.");

        Base64.Decoder urlDecoder = Base64.getUrlDecoder();
        byte[] decodedPayload = urlDecoder.decode(splits[1].getBytes(StandardCharsets.UTF_8));
        JwtPayload jwtPayload = objectMapper.readValue(decodedPayload, JwtPayload.class);

        return jwtPayload.getUserDTO();
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
    @NoArgsConstructor
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
