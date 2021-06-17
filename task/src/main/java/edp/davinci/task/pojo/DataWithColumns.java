package edp.davinci.task.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class DataWithColumns {
    List<Map<String,Object>> data = new ArrayList<>();
    List<String> columns = new ArrayList<>();
}
