package edp.davinci.task.service.impl;

import edp.davinci.core.dao.UserMapper;
import edp.davinci.core.dao.entity.User;
import edp.davinci.core.dao.entity.View;
import edp.davinci.task.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    @Override
    public User getUser(long id) {
        User user = userMapper.selectByPrimaryKey(id);
        if (null == user) {
            log.error("User({}) not found", id);
        }
        return user;
    }
}
