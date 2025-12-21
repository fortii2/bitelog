package me.forty2.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
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
import me.forty2.utils.RedisConstants;
import me.forty2.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result loginOrRegister(LoginFormDTO loginForm) {

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

        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString())
        );

        UUID token = UUID.randomUUID();
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);

        return Result.ok(token);
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
