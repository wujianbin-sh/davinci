package edp.davinci.task.service.impl;

import edp.davinci.commons.util.CollectionUtils;
import edp.davinci.core.dao.RelRoleUserMapper;
import edp.davinci.core.dao.RelRoleViewMapper;
import edp.davinci.core.dao.entity.RelRoleUser;
import edp.davinci.core.dao.entity.RelRoleUserExample;
import edp.davinci.core.dao.entity.RelRoleView;
import edp.davinci.core.dao.entity.RelRoleViewExample;
import edp.davinci.task.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    RelRoleViewMapper relRoleViewMapper;

    @Autowired
    RelRoleUserMapper relRoleUserMapper;

    @Override
    public List<RelRoleView> getViewRoles(Long viewId, Long userId) {
        RelRoleUserExample relRoleUserExample = new RelRoleUserExample();
        relRoleUserExample.createCriteria().andUserIdEqualTo(userId);
        List<RelRoleUser> relRoleUsers = relRoleUserMapper.selectByExample(relRoleUserExample);

        RelRoleViewExample relRoleViewExample = new RelRoleViewExample();
        relRoleViewExample.createCriteria().andViewIdEqualTo(viewId);
        List<RelRoleView> relRoleViews = relRoleViewMapper.selectByExample(relRoleViewExample);

        if (CollectionUtils.isEmpty(relRoleUsers) || CollectionUtils.isEmpty(relRoleViews)) {
            return Collections.emptyList();
        }

        List<RelRoleView> resList = new ArrayList<>();
        relRoleViews.forEach(rrv -> {
            if (relRoleUsers.stream().filter(rru -> rrv.getRoleId().equals(rru.getRoleId())).findFirst().orElse(null) != null) {
                resList.add(rrv);
            }
        });

        return resList;
    }
}
