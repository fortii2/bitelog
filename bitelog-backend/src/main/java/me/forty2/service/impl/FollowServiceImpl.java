package me.forty2.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.forty2.entity.Follow;
import me.forty2.mapper.FollowMapper;
import me.forty2.service.FollowService;
import org.springframework.stereotype.Service;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

}
