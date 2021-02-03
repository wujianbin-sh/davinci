package edp.davinci.server.model;

import lombok.Data;

@Data
public class MOAEmployee {
    private String employeeID;
    // userName is email
    private String userName;
    private String realName;
}
