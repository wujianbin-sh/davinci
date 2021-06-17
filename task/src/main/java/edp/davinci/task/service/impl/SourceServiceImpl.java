package edp.davinci.task.service.impl;

import edp.davinci.core.dao.SourceMapper;
import edp.davinci.core.dao.entity.Source;
import edp.davinci.core.dao.entity.View;
import edp.davinci.task.service.SourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SourceServiceImpl implements SourceService {

    @Autowired
    SourceMapper sourceMapper;

    @Override
    public Source getSource(long id) {
        Source source = sourceMapper.selectByPrimaryKey(id);
        if (null == source) {
            log.error("Source({}) not found", id);
        }
        return source;
    }
}
