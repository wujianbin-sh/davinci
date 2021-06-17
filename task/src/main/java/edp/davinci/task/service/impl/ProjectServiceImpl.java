package edp.davinci.task.service.impl;

import edp.davinci.commons.util.CollectionUtils;
import edp.davinci.core.dao.*;
import edp.davinci.core.dao.entity.*;
import edp.davinci.core.enums.UserOrgRoleEnum;
import edp.davinci.task.mapper.ProjectExtendMapper;
import edp.davinci.task.pojo.ProjectPermission;
import edp.davinci.task.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    ProjectExtendMapper projectExtendMapper;

    @Autowired
    OrganizationMapper organizationMapper;

    @Autowired
    RelUserOrganizationMapper relUserOrganizationMapper;

    @Autowired
    RelProjectAdminMapper relProjectAdminMapper;

    @Override
    public boolean isMaintainer(Long projectId, Long userId) {

        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            return false;
        }

        Project project = projectExtendMapper.selectByPrimaryKey(projectId);
        if (project == null) {
            return false;
        }

        boolean isProjectCreator = project.getUserId().equals(user.getId()) && !project.getIsTransfer();
        if (isProjectCreator) {
            return true;
        }

        Organization org = organizationMapper.selectByPrimaryKey(project.getOrgId());

        RelUserOrganizationExample relUserOrganizationExample = new RelUserOrganizationExample();
        relUserOrganizationExample.createCriteria().andUserIdEqualTo(userId).andOrgIdEqualTo(org.getId());
        List<RelUserOrganization> relUserOrgs = relUserOrganizationMapper.selectByExample(relUserOrganizationExample);
        RelUserOrganization relUserOrg = relUserOrgs.size() > 0 ? relUserOrgs.get(0) : null;

        RelProjectAdminExample relProjectAdminExample = new RelProjectAdminExample();
        relProjectAdminExample.createCriteria().andUserIdEqualTo(userId).andProjectIdEqualTo(projectId);
        List<RelProjectAdmin> relProjectAdmins = relProjectAdminMapper.selectByExample(relProjectAdminExample);

        boolean isProjectAdmin = !CollectionUtils.isEmpty(relProjectAdmins);
        boolean isOrgOwner = relUserOrg != null && relUserOrg.getRole() == UserOrgRoleEnum.OWNER.getRole();
        boolean isMaintainer = isProjectAdmin || isOrgOwner;

        if (isMaintainer) {
            return true;
        }

        if (org.getMemberPermission() < (short) 1
                && !project.getVisibility()) {
            log.error("User({}) have not permission to visit project({})", user.getId(), projectId);
            throw new RuntimeException("User have not permission to visit project");
        }

        return false;
    }

    @Override
    public ProjectPermission getPermission(Long projectId, Long userId) {
        return projectExtendMapper.getProjectPermission(projectId, userId);
    }
}
