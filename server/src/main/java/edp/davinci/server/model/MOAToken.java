package edp.davinci.server.model;

import lombok.Data;

@Data
public class MOAToken {
    private String sysCode;
    private String userToken;
    private String token;
    private String ssoToken;
}
