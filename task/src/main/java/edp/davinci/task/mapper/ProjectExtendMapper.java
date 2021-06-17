package edp.davinci.task.mapper;

import edp.davinci.core.dao.ProjectMapper;
import edp.davinci.task.pojo.ProjectPermission;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

@Component
public interface ProjectExtendMapper extends ProjectMapper {

    @Select({"select" +
            "   ifnull(max(rrp.viz_permission), 0)      as vizPermission," +
            "   ifnull(max(rrp.widget_permission), 0)   as widgetPermission," +
            "   ifnull(max(rrp.view_permission), 0)     as viewPermission," +
            "   ifnull(max(rrp.source_permission), 0)   as sourcePermission," +
            "   ifnull(max(rrp.schedule_permission), 0) as schedulePermission," +
            "   ifnull(max(rrp.share_permission), 0)    as sharePermission," +
            "   ifnull(max(rrp.download_permission), 0) as downloadPermission" +
            "   from rel_role_project rrp left join rel_role_user rru on rru.role_id = rrp.role_id" +
            "   where rrp.project_id = #{projectId} and rru.user_id = #{userId}"
    })
    ProjectPermission getProjectPermission(Long projectId, Long userId);
}
