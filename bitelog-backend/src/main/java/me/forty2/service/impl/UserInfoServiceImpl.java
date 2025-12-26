package me.forty2.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.forty2.entity.UserInfo;
import me.forty2.mapper.UserInfoMapper;
import me.forty2.service.UserInfoService;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

}
