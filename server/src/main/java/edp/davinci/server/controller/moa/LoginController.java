package edp.davinci.server.controller.moa;


import edp.davinci.core.dao.entity.User;
import edp.davinci.server.commons.Constants;
import edp.davinci.server.controller.ResultMap;
import edp.davinci.server.dto.user.UserLoginResult;
import edp.davinci.server.exception.NotFoundException;
import edp.davinci.server.model.MOAEmployee;
import edp.davinci.server.model.MOAToken;
import edp.davinci.server.model.TokenEntity;
import edp.davinci.server.service.UserService;
import edp.davinci.server.util.MOAUtils;
import edp.davinci.server.util.TokenUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "moa login", basePath = Constants.BASE_API_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces =
        MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses({
        @ApiResponse(code = 400, message = "password is wrong"),
        @ApiResponse(code = 404, message = "user not found")
})
@RestController("moaLoginController")
@Slf4j
@RequestMapping(value = Constants.BASE_API_PATH + "/moa", consumes = MediaType.APPLICATION_JSON_VALUE, produces =
        MediaType.APPLICATION_JSON_UTF8_VALUE)
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private MOAUtils moaUtils;

    @Autowired
    private Environment environment;

    /**
     * 宜办公登录
     *
     * @param token
     * @return
     */
    @ApiOperation(value = "Login into the server and return token")
    @PostMapping(value = "/login")
    public ResponseEntity login(@RequestBody MOAToken token) {

        MOAEmployee employee = moaUtils.getUserinfo(token);

        if (employee == null) {
            throw new NotFoundException("User not found");
        }

        User user = userService.userLogin(employee);
        TokenEntity tokenDetail = new TokenEntity(user.getUsername(), user.getPassword());

        if (!user.getActive()) {
            log.error("User is not active, username:{}", user.getUsername());
            ResultMap resultMap = new ResultMap(tokenUtils).failWithToken(tokenUtils.generateToken(tokenDetail)).message("This user is not active");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        UserLoginResult userLoginResult = new UserLoginResult(user);
        String statistic_open = environment.getProperty("statistic.enable");
        if ("true".equalsIgnoreCase(statistic_open)) {
            userLoginResult.setStatisticOpen(true);
        }

        return ResponseEntity.ok(new ResultMap().success(tokenUtils.generateToken(tokenDetail)).payload(userLoginResult));
    }
}
