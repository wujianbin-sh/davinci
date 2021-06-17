package edp.davinci.task.service;

import edp.davinci.task.pojo.ProjectPermission;

public interface ProjectService {
    boolean isMaintainer(Long projectId, Long userId);
    ProjectPermission getPermission(Long projectId, Long userId);
}
