package me.forty2.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.forty2.entity.Blog;
import me.forty2.mapper.BlogMapper;
import me.forty2.service.BlogService;
import org.springframework.stereotype.Service;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements BlogService {

}
