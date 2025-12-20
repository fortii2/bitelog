package me.forty2.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.forty2.dto.LoginFormDTO;
import me.forty2.dto.UserDTO;
import me.forty2.entity.User;
import me.forty2.mapper.UserMapper;
import me.forty2.service.UserService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    @Override
    public UserDTO loginOrRegister(LoginFormDTO loginForm, String validCodeFromSession) {

        User user = getBaseMapper().selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, loginForm.getPhone()));

        boolean isCodeValid = loginForm.getCode() != null && loginForm.getCode().equals(validCodeFromSession);

        boolean isPasswordValid = user != null && user.getPassword() != null && loginForm.getPassword() != null && user.getPassword().equals(loginForm.getPassword());

        if (!(isCodeValid || isPasswordValid)) {
            return null;
        }

        if (user == null) {
            user = new User();
            user.setPhone(loginForm.getPhone());
            user.setNickName(RandomUtil.randomString(6));
            this.save(user);
        }

        return BeanUtil.copyProperties(user, UserDTO.class);
    }
}
