package edp.davinci.core.util;

import com.alibaba.fastjson.JSONObject;
import edp.davinci.dto.userDto.MOAToken;
import edp.davinci.model.MOAEmployee;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class MOAUtils {

    @Data
    private static class ResultMap {
        String code;
        String message;
        MOAEmployee content;
        String platformCode;
    }

    @Autowired
    private RestTemplate restTemplate;

    @Value("${moa.base-url}")
    private String baseUrl;

    public MOAEmployee getUserinfo(MOAToken token) {

        String requestBody = JSONObject.toJSONString(token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity(requestBody, headers);

        try {

            ResponseEntity<ResultMap> responseEntity =
                    restTemplate.postForEntity(UriComponentsBuilder.fromHttpUrl(baseUrl + "/entryChk" +
                            "/getUserinfo")
                            .build().toString(), requestEntity, ResultMap.class);

            HttpStatus statusCode = responseEntity.getStatusCode();
            if (!statusCode.equals(HttpStatus.OK)) {
                log.error("MOA /entryChk/getUserinfo error, requestBody:" + requestBody + ", httpStatus:" + statusCode.value());
                return null;
            }

            ResultMap resultMap = responseEntity.getBody();
            if ("00".equalsIgnoreCase(resultMap.getCode())) {
                return resultMap.getContent();
            }

            log.error("MOA /entryChk/getUserinfo error, requestBody:" + requestBody + ", responseBody:" + JSONObject.toJSONString(resultMap));

        }catch (Exception e) {
            log.error("MOA /entryChk/getUserinfo error, requestBody:" + requestBody, e);
        }

        return null;
    }
}
