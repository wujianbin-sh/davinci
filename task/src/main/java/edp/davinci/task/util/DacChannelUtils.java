/*
 * <<
 *  Davinci
 *  ==
 *  Copyright (C) 2016 - 2020 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package edp.davinci.task.util;

import edp.davinci.commons.util.StringUtils;
import edp.davinci.task.pojo.DacChannel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
@Component
@ConfigurationProperties(prefix = "data-auth-center", ignoreInvalidFields = true)
public class DacChannelUtils {

    @Getter
    private static final Map<String, DacChannel> dacMap = new HashMap<>();

    @Setter
    @Getter
    private List<DacChannel> channels = new ArrayList<>();

    @Autowired
    private RestTemplate restTemplate;

    public void loadDacMap() {
        if (null != channels) {
            Map<String, List<DacChannel>> map = channels.stream()
                    .filter(c -> !StringUtils.isEmpty(c.getName()) && !StringUtils.isEmpty(c.getBaseUrl()))
                    .collect(groupingBy(DacChannel::getName));

            if (!CollectionUtils.isEmpty(map)) {
                map.forEach((k, v) -> dacMap.put(k.trim(), v.get(v.size() - 1)));
            }
        }
    }

    public List<Object> getData(String dacName, String bizId, String email) {
        if (dacMap.containsKey(dacName) && !StringUtils.isEmpty(email)) {
            DacChannel channel = dacMap.get(dacName);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("authCode", channel.getAuthCode());
            params.add("email", email);

            try {
                ResponseEntity<Map> result = restTemplate.getForEntity(UriComponentsBuilder.
                                fromHttpUrl(channel.getBaseUrl() + "/bizs/{bizId}/data")
                                .queryParams(params)
                                .build().toString(),
                        Map.class, bizId);

                if (result.getStatusCode().equals(HttpStatus.OK)) {
                    Map<String, Object> resultMap = result.getBody();
                    return (List<Object>) resultMap.get("payload");
                }
            } catch (RestClientException e) {
				log.error(e.toString(), e);
            }
        }
        return null;
    }
}
