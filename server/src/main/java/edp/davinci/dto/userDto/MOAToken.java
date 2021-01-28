package edp.davinci.dto.userDto;

import lombok.Data;

@Data
public class MOAToken {
    private String sysCode;
    private String userToken;
    private String token;
    private String ssoToken;
}
