package me.forty2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import me.forty2.dto.LoginFormDTO;
import me.forty2.dto.Result;
import me.forty2.entity.User;

public interface UserService extends IService<User> {

    Result loginOrRegister(LoginFormDTO loginForm) throws Exception;

    Result sendCode(String phone);
}
