package me.forty2.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.forty2.dto.LoginFormDTO;
import me.forty2.dto.Result;
import me.forty2.dto.UserDTO;
import me.forty2.entity.User;
import me.forty2.mapper.UserMapper;
import me.forty2.service.UserService;
import me.forty2.utils.JwtUtils;
import me.forty2.utils.RedisConstants;
import me.forty2.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Value("${my-jwt.secret}")
    private String secret;

    @Override
    public Result loginOrRegister(LoginFormDTO loginForm) throws Exception {

        String validCodeFromRedis = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + loginForm.getPhone());

        if (loginForm.getCode() == null && loginForm.getPassword() == null) {
            return Result.fail("valid Code and Password are both empty.");
        }

        if (loginForm.getCode() != null && RegexUtils.isCodeInvalid(loginForm.getCode())) {
            return Result.fail("Valid Code is Invalid.");
        }

        User user = getBaseMapper().selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, loginForm.getPhone()));

        boolean isCodeValid = loginForm.getCode() != null && loginForm.getCode().equals(validCodeFromRedis);

        boolean isPasswordValid = user != null && user.getPassword() != null && loginForm.getPassword() != null && user.getPassword().equals(loginForm.getPassword());

        if (!(isCodeValid || isPasswordValid)) {
            return Result.fail("No auth way.");
        }

        if (user == null) {
            user = new User();
            user.setPhone(loginForm.getPhone());
            user.setNickName(RandomUtil.randomString(6));
            this.save(user);
        }

        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

        return Result.ok(JwtUtils.generateJWT(userDTO, secret));
    }

    @Override
    public Result sendCode(String phone) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            Result.fail("Phone is Invalid.");
        }

        String validCode = RandomUtil.randomNumbers(6);
        // TODO using real service rather than log
        log.info("valid code is: {}", validCode);

        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, validCode, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);

        return Result.ok();
    }
}
