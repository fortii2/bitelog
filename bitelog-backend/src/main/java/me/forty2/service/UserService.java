package me.forty2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import me.forty2.dto.LoginFormDTO;
import me.forty2.dto.UserDTO;
import me.forty2.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface UserService extends IService<User> {

    UserDTO loginOrRegister(LoginFormDTO loginForm, String validCodeFromSession);
}
