package me.forty2.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.forty2.dto.UserDTO;
import me.forty2.utils.CommonConstants;
import me.forty2.utils.JwtUtils;
import me.forty2.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

public class RefreshInterceptor implements HandlerInterceptor {

    private String secret;

    public RefreshInterceptor(String jwtSecret) {
        this.secret = jwtSecret;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String auth = request.getHeader(CommonConstants.AUTHORIZATION);
        if (auth == null) {
            return true;
        }

        UserDTO userDTO = JwtUtils.getPayload(auth, secret);

        if (userDTO == null) {
            return true;
        }

        UserHolder.saveUser(userDTO);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
