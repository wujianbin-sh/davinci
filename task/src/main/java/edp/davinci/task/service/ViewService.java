package edp.davinci.task.service;

import edp.davinci.core.dao.entity.User;
import edp.davinci.core.dao.entity.View;
import edp.davinci.task.pojo.DataWithColumns;
import edp.davinci.task.pojo.SqlVariable;
import edp.davinci.task.pojo.WidgetQueryParam;

import java.util.List;

public interface ViewService {
    View getView(Long id);
    List<SqlVariable> getViewVariables(String variable);
    DataWithColumns getData(Long id, WidgetQueryParam queryParam, User user);
}
