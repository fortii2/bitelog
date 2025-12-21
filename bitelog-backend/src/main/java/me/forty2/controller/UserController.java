package me.forty2.controller;


import lombok.extern.slf4j.Slf4j;
import me.forty2.dto.LoginFormDTO;
import me.forty2.dto.Result;
import me.forty2.entity.UserInfo;
import me.forty2.service.UserInfoService;
import me.forty2.service.UserService;
import me.forty2.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserInfoService userInfoService;

    /**
     * send valid code
     *
     * @param phone phone number. valid by Chinese rules. Like: 15133013765
     * @return {@link Result}
     */
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone) {
        return userService.sendCode(phone);
    }

    /**
     * if is a new phone number, register. Otherwise, login.
     * login is actually add userDTO to session.
     *
     * @param loginForm data from browser
     * @return {@link Result}
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm) {
        return userService.loginOrRegister(loginForm);
    }

    /**
     * 登出功能
     *
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout() {
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    @GetMapping("/me")
    public Result me() {
        return Result.ok(UserHolder.getUser());
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }
}
