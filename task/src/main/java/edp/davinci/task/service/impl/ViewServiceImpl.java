package edp.davinci.task.service.impl;

import edp.davinci.commons.util.CollectionUtils;
import edp.davinci.commons.util.DateUtils;
import edp.davinci.commons.util.JSONUtils;
import edp.davinci.commons.util.StringUtils;
import edp.davinci.core.dao.ViewMapper;
import edp.davinci.core.dao.entity.RelRoleView;
import edp.davinci.core.dao.entity.Source;
import edp.davinci.core.dao.entity.User;
import edp.davinci.core.dao.entity.View;
import edp.davinci.data.parser.ParserFactory;
import edp.davinci.data.parser.StatementParser;
import edp.davinci.data.pojo.DataColumn;
import edp.davinci.data.pojo.DataResult;
import edp.davinci.data.pojo.PagingParam;
import edp.davinci.data.pojo.SqlQueryParam;
import edp.davinci.data.provider.DataProviderFactory;
import edp.davinci.task.enums.SqlVariableTypeEnum;
import edp.davinci.task.enums.SqlVariableValueTypeEnum;
import edp.davinci.task.pojo.*;
import edp.davinci.task.service.*;
import edp.davinci.task.util.AuthVarUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static edp.davinci.task.commons.Constants.AUTH_PERMISSION;
import static edp.davinci.task.commons.Constants.NO_AUTH_PERMISSION;

@Slf4j
@Service
public class ViewServiceImpl implements ViewService {

    @Autowired
    ViewMapper viewMapper;

    @Autowired
    UserService userService;

    @Autowired
    SourceService sourceService;

    @Autowired
    ProjectService projectService;

    @Autowired
    RoleService roleService;

    @Autowired
    AuthVarUtils authVarUtils;

    DateUtils dateUtils = new DateUtils();

    private static final JexlEngine jexl = new JexlBuilder().cache(512).strict(true).silent(false).create();

    @Override
    public View getView(Long id) {
        View view = viewMapper.selectByPrimaryKey(id);
        if (null == view) {
            log.error("View({}) not found", id);
        }
        return view;
    }

    @Override
    public List<SqlVariable> getViewVariables(String variable) {
        if (StringUtils.isEmpty(variable)) {
            return Collections.emptyList();
        }

        try {
            return JSONUtils.toObjectArray(variable, SqlVariable.class);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }

        return Collections.emptyList();
    }

    @Override
    public DataWithColumns getData(Long id, WidgetQueryParam queryParam, User user) {

        View view = getView(id);
        boolean isMaintainer = projectService.isMaintainer(view.getProjectId(), user.getId());
        if (!isMaintainer) {
            ProjectPermission permission = projectService.getPermission(view.getProjectId(), user.getId());
            if (permission.getVizPermission() < 1) {
                log.error("User({}) have not permission to visit project({}) viz", user.getId(), view.getProjectId());
                throw new RuntimeException("User have not permission to visit project viz");
            }
        }

        Source source = sourceService.getSource(view.getSourceId());

        List<Param> params = queryParam.getParams();
        params.forEach(p -> {
            String exprType = p.getExprType();
            String expr = p.getExpr();
            if (StringUtils.isEmpty(p.getExprType()) || StringUtils.isEmpty(expr)) {
                return;
            }

            switch (exprType){
                case "date":
                    JexlExpression e = jexl.createExpression(expr);
                    JexlContext context = new MapContext();
                    context.set("dateUtils", dateUtils);
                    p.setValue(e.evaluate(context).toString());
                default:
            }
        });

        StatementParser parser = ParserFactory.getParser(source.getType());
        SqlQueryParam sqlQueryParam = SqlQueryParam.builder()
                .limit(queryParam.getLimit())
                .pageNo(queryParam.getPageNo())
                .pageSize(queryParam.getPageSize())
                .isMaintainer(isMaintainer)
                .nativeQuery(queryParam.isNativeQuery())
                .aggregators(queryParam.getAggregators())
                .groups(queryParam.getGroups())
                .filters(queryParam.getFilters())
                .orders(queryParam.getOrders())
                .type(queryParam.getType())
                .build();

        String statement = view.getSql();
        List<SqlVariable> variables = getViewVariables(view.getVariable());
        List<RelRoleView> roleViews = roleService.getViewRoles(id, user.getId());

        Map<String, List<String>> authParams = new HashMap<>();
        if (isMaintainer) {
            authParams = null;
        } else {
            setAuthVarValue(authParams, variables, roleViews, user);
        }

        Map<String, Object> queryParams = new HashMap<>();
        setQueryVarValue(queryParams, variables, params);

        statement = parser.preParse(statement, sqlQueryParam, authParams, queryParams, source, user);
        String sWithSysVar = parser.parseSystemVars(statement, sqlQueryParam, source, user);
        String sWithAuthVar = parser.parseAuthVars(sWithSysVar, sqlQueryParam, authParams, queryParams, source, user);
        String sWithQueryVar = parser.parseQueryVars(sWithAuthVar, sqlQueryParam, queryParams, authParams, source, user);

        List<String> queryStatements = parser.getQueryStatement(sWithQueryVar, sqlQueryParam, source, user);

        DataWithColumns dataWithColumns = new DataWithColumns();
        if (CollectionUtils.isEmpty(queryStatements)) {
            return dataWithColumns;
        }

        PagingParam paging = new PagingParam(sqlQueryParam.getPageNo(),
                sqlQueryParam.getPageSize(), sqlQueryParam.getLimit());
        DataResult dataResult = DataProviderFactory.getProvider(source.getType()).getData(source,
                queryStatements.get(queryStatements.size() - 1),
                paging, user);

        Set<String> excludeColumns = getExcludeColumns(roleViews);

        return dataResult2DataWithColumns(dataResult, excludeColumns);
    }

    private void setAuthVarValue(Map<String, List<String>> authParams, List<SqlVariable> variables,
                                 List<RelRoleView> roleViewList, User user) {

        if (CollectionUtils.isEmpty(variables)) {
            return;
        }

        List<SqlVariable> authVars = variables.stream()
                .filter(v -> SqlVariableTypeEnum.AUTHVAR == SqlVariableTypeEnum.typeOf(v.getType())).collect(Collectors.toList());

        roleViewList.forEach(r -> {

            if (StringUtils.isEmpty(r.getRowAuth())) {
                return;
            }

            List<AuthParamValue> paramValues = JSONUtils.toObjectArray(r.getRowAuth(), AuthParamValue.class);
            authVars.forEach((v) -> {
                List<Object> defaultValues = v.getDefaultValues();
                Optional<AuthParamValue> optional = paramValues.stream().filter(paramValue -> paramValue.getName().equals(v.getName()))
                        .findFirst();

                if (defaultValues == null) {
                    if (optional.isPresent()) {
                        AuthParamValue paramValue = optional.get();
                        if (paramValue.isEnable()) {
                            if (CollectionUtils.isEmpty(paramValue.getValues())) {
                                v.setDefaultValues(Arrays.asList(new String[]{NO_AUTH_PERMISSION}));
                            } else {
                                v.setDefaultValues(paramValue.getValues());
                            }
                        } else {
                            v.setDefaultValues(Arrays.asList(new String[]{AUTH_PERMISSION}));
                        }
                    } else {
                        v.setDefaultValues(Arrays.asList(new String[]{AUTH_PERMISSION}));
                    }
                    return;
                }

                if (!optional.isPresent()) {
                    v.setDefaultValues(Arrays.asList(new String[]{AUTH_PERMISSION}));
                    return;
                }

                AuthParamValue paramValue = optional.get();
                List<Object> values = paramValue.getValues();
                if (paramValue.isEnable()) {
                    if (!CollectionUtils.isEmpty(values)) {
                        boolean denied = defaultValues.size() == 1 && defaultValues.get(0).equals(NO_AUTH_PERMISSION);
                        if (denied) {
                            v.setDefaultValues(values);
                            return;
                        }

                        boolean permission = defaultValues.size() == 1 && defaultValues.get(0).equals(AUTH_PERMISSION);
                        if (permission) {
                            return;
                        }

                        defaultValues.addAll(values);

                    }
                } else {
                    v.setDefaultValues(Arrays.asList(new String[]{AUTH_PERMISSION}));
                }

            });
        });

        for (SqlVariable var : authVars) {
            List<Object> defaultValues = var.getDefaultValues();
            if (defaultValues != null) {
                boolean permission = defaultValues.size() == 1 && defaultValues.get(0).equals(AUTH_PERMISSION);
                if (permission) {
                    var.setDefaultValues(null);
                }

                boolean denied = defaultValues.size() == 1 && defaultValues.get(0).equals(NO_AUTH_PERMISSION);
                if (denied) {
                    var.setDefaultValues(Collections.emptyList());
                }
            }

            List<String> values = authVarUtils.getValue(var, user.getEmail());
            String varName = var.getName();
            if (values != null && values.size() == 0) {
                authParams.put(varName, Arrays.asList(new String[]{NO_AUTH_PERMISSION}));
            } else {
                authParams.put(varName, values);
            }
        }
    }

    private void setQueryVarValue(Map<String, Object> queryParams, List<SqlVariable> variables, List<Param> params) {
        variables.forEach(var -> {
            SqlVariableTypeEnum typeEnum = SqlVariableTypeEnum.typeOf(var.getType());
            if (typeEnum != SqlVariableTypeEnum.QUERYVAR) {
                return;
            }

            String varName = var.getName().trim();
            Object value = null;
            if (!CollectionUtils.isEmpty(params)) {
                Param param = null;
                Optional<Param> optional =
                        params.stream().filter(p -> p.getName().equals(varName)).findFirst();
                if (optional.isPresent()) {
                    param = optional.get();
                    value = SqlVariableValueTypeEnum.getValue(var.getValueType(), param.getValue(), var.isUdf());
                }
            }

            if (value == null) {
                queryParams.put(varName,
                        SqlVariableValueTypeEnum.getValues(var.getValueType(), var.getDefaultValues(), var.isUdf()));
                return;
            }

            queryParams.put(varName, value);
        });
    }

    private Set<String> getExcludeColumns(List<RelRoleView> roleViewList) {
        if (CollectionUtils.isEmpty(roleViewList)) {
            return Collections.emptySet();
        }

        Set<String> columns = new HashSet<>();
        boolean isFullAuth = false;
        for (RelRoleView r : roleViewList) {
            if (StringUtils.isEmpty(r.getColumnAuth())) {
                isFullAuth = true;
                break;
            }

            List<String> authColumns = JSONUtils.toObjectArray(r.getColumnAuth(), String.class);
            if (CollectionUtils.isEmpty(authColumns)) {
                isFullAuth = true;
                break;
            }

            columns.addAll(authColumns);
        }

        if (isFullAuth) {
            return Collections.emptySet();
        }

        for (RelRoleView r : roleViewList) {
            List<String> authColumns = JSONUtils.toObjectArray(r.getColumnAuth(), String.class);
            Iterator<String> iterator = columns.iterator();
            while (iterator.hasNext()) {
                String column = iterator.next();
                if (!authColumns.contains(column)) {
                    iterator.remove();
                }
            }
        }

        return columns;
    }

    private DataWithColumns dataResult2DataWithColumns(DataResult dataResult, Set<String> excludeColumns) {
        List<List<Object>> dataResultData = dataResult.getData();
        List<DataColumn> dataResultHeader = dataResult.getHeader();

        DataWithColumns dataWithColumns = new DataWithColumns();

        if (CollectionUtils.isEmpty(dataResultData)) {
            dataWithColumns.setColumns(dataResultHeader.stream().filter(h -> !excludeColumns.contains(h)).map(h -> h.getName()).collect(Collectors.toList()));
            return dataWithColumns;
        }


        List<String> columnList = new ArrayList<>();
        dataWithColumns.setColumns(columnList);

        List<Map<String, Object>> dataList = new ArrayList<>();
        dataWithColumns.setData(dataList);

        for (int i = 0; i < dataResultData.size(); i++) {

            List<Object> data = dataResultData.get(i);

            Map<String, Object> dataMap = new HashMap<>();
            for (int j = 0; j < dataResultHeader.size(); j++) {
                DataColumn column = dataResultHeader.get(j);
                String columnName = column.getName();
                if (excludeColumns.contains(columnName)) {
                    continue;
                }

                if (i == 0) {
                    columnList.add(columnName);
                }

                dataMap.put(columnName, data.get(j));
            }

            dataList.add(dataMap);
        }

        return dataWithColumns;
    }
}
