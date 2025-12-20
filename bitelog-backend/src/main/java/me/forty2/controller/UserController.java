package me.forty2.controller;


import cn.hutool.core.util.RandomUtil;
import me.forty2.dto.LoginFormDTO;
import me.forty2.dto.Result;
import me.forty2.dto.UserDTO;
import me.forty2.entity.UserInfo;
import me.forty2.service.UserInfoService;
import me.forty2.service.UserService;
import lombok.extern.slf4j.Slf4j;
import me.forty2.utils.RegexUtils;
import me.forty2.utils.UserHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

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

    @Resource
    private UserService userService;

    @Resource
    private UserInfoService userInfoService;

    /**
     * send valid code
     *
     * @param phone   phone number. valid by Chinese rules. Like: 15133013765
     * @param session session
     * @return {@link Result}
     */
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            Result.fail("Phone is Invalid.");
        }

        String validCode = RandomUtil.randomNumbers(6);
        // TODO using real service rather than log
        log.info("valid code is: {}", validCode);

        session.setAttribute("phone", phone);
        session.setAttribute("validCode", validCode);

        return Result.ok();
    }

    /**
     * if is a new phone number, register. Otherwise, login.
     * login is actually add userDTO to session.
     *
     * @param loginForm data from browser
     * @param session   session
     * @return {@link Result}
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {

        if (loginForm.getCode() == null && loginForm.getPassword() == null) {
            return Result.fail("valid Code and Password are both empty.");
        }

        if (loginForm.getPhone() == null || (!loginForm.getPhone().equals(session.getAttribute("phone")))) {
            return Result.fail("Phone is Invalid or not same phone.");
        }

        if (loginForm.getCode() != null && RegexUtils.isCodeInvalid(loginForm.getCode())) {
            return Result.fail("Valid Code is Invalid.");
        }


        UserDTO userDTO = userService.loginOrRegister(loginForm, (String) session.getAttribute("validCode"));

        if (userDTO == null) {
            return Result.fail("Wrong auth.");
        }

        session.setAttribute("login", userDTO);

        return Result.ok();
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
