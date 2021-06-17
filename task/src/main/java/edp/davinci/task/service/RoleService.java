package edp.davinci.task.service;

import edp.davinci.core.dao.entity.RelRoleView;

import java.util.List;

public interface RoleService {
    List<RelRoleView> getViewRoles(Long viewId, Long userId);
}
