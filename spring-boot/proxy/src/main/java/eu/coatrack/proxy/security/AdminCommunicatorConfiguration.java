package eu.coatrack.proxy.security;

/*-
 * #%L
 * coatrack-proxy
 * %%
 * Copyright (C) 2013 - 2021 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Builds and provides the required configuration details for communication with the CoatRack admin server.
 *
 * @author Christoph Baier
 */

@Configuration("securityConfiguration")
public class AdminCommunicatorConfiguration {

    @Value("${proxy-id}")
    private String gatewayId = "";

    @Value("${ygg.admin.api-base-url}")
    private String adminBaseUrl;

    @Value("${ygg.admin.resources.search-api-key-list}")
    private String adminResourceToSearchForApiKeyList;

    @Value("${ygg.admin.resources.search-api-keys-by-token-value}")
    private String adminResourceToSearchForApiKeys;

    @Value("${ygg.admin.resources.search-service-by-api-key-value}")
    private String adminResourceToGetServiceByApiKeyValue;

    private final SecurityUtil securityUtil;

    private String
            apiKeyListRequestUrl,
            apiKeyRequestUrlWithoutApiKeyValue,
            serviceApiRequestUrlWithoutApiKeyValue;

    public AdminCommunicatorConfiguration(SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
    }

    @PostConstruct
    private void initUrls() {
        apiKeyListRequestUrl = securityUtil.attachGatewayApiKeyToUrl(adminBaseUrl
                + adminResourceToSearchForApiKeyList);
        apiKeyRequestUrlWithoutApiKeyValue = securityUtil.attachGatewayApiKeyToUrl(adminBaseUrl
                + adminResourceToSearchForApiKeys);
        serviceApiRequestUrlWithoutApiKeyValue = securityUtil.attachGatewayApiKeyToUrl(adminBaseUrl
                + adminResourceToGetServiceByApiKeyValue);
    }

    public String getGatewayId() {
        return gatewayId;
    }

    public String getApiKeyListRequestUrl() {
        return apiKeyListRequestUrl;
    }

    public String getApiKeyRequestUrlWithoutApiKeyValue() {
        return apiKeyRequestUrlWithoutApiKeyValue;
    }

    public String getServiceApiRequestUrlWithoutApiKeyValue() {
        return serviceApiRequestUrlWithoutApiKeyValue;
    }
}
