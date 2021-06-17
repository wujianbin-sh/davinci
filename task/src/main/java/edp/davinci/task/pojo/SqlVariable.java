package edp.davinci.task.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SqlVariable {
    private String name;
    private String type;    //变量类型 query/auth
    private String valueType;   //变量值类型 string/number/boolean/date/sql
    private boolean udf;    //是否使用表达式
    private List<Object> defaultValues; //默认值
    private SqlVariableChannel channel; //data-auth-center(dac) parameter
}
