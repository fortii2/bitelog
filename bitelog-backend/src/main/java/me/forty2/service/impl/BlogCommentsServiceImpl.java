package me.forty2.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.forty2.entity.BlogComments;
import me.forty2.mapper.BlogCommentsMapper;
import me.forty2.service.BlogCommentsService;
import org.springframework.stereotype.Service;

@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements BlogCommentsService {

}
