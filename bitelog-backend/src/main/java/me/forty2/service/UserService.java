package me.forty2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import me.forty2.dto.LoginFormDTO;
import me.forty2.dto.Result;
import me.forty2.entity.User;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface UserService extends IService<User> {

    Result loginOrRegister(LoginFormDTO loginForm);

    Result sendCode(String phone);
}
