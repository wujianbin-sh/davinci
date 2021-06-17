package edp.davinci.task.pojo;

import edp.davinci.data.pojo.Aggregator;
import edp.davinci.data.pojo.Filter;
import edp.davinci.data.pojo.Order;
import lombok.Data;

import java.util.List;

@Data
public class WidgetQueryParam {
    private List<String> groups;
    private List<Aggregator> aggregators;
    private List<Order> orders;
    private List<Filter> filters;
    private List<Param> params;
    private Boolean cache = false;
    private Long expired = -1L;
    private Boolean flush = false;
    private int limit = 0;
    private int pageNo = -1;
    private int pageSize = -1;
    private int totalCount = 0;
    private final String type = "query";

    private boolean nativeQuery = false;
}
