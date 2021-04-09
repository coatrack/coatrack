package eu.coatrack.proxy.security;

/*-
 * #%L
 * coatrack-proxy
 * %%
 * Copyright (C) 2013 - 2020 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
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

import eu.coatrack.api.Proxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * This class provides information required to fetch API keys from the CoatRack admin server.
 *
 * @author Christoph Baier
 */

@Service("securityUtil")
public class UrlResourcesProvider {

    @Value("${proxy-id}")
    private String gatewayId = "";

    @Value("${ygg.admin.api-base-url}")
    private String adminBaseUrl;

    @Value("${ygg.admin.resources.get-api-key-list-by-gatewayId}")
    private String adminResourceToFetchApiKeyList;

    @Value("${ygg.admin.resources.search-api-keys-by-token-value}")
    private String adminResourceToFetchSingleApiKey;

    private String
            apiKeyListRequestUrl,
            apiKeyRequestUrlWithoutApiKeyValueAndGatewayId;

    @PostConstruct
    private void initUrls() {
        apiKeyListRequestUrl = attachGatewayIdToUrl(adminBaseUrl + adminResourceToFetchApiKeyList);
        apiKeyRequestUrlWithoutApiKeyValueAndGatewayId = adminBaseUrl + adminResourceToFetchSingleApiKey;
    }

    public String getApiKeyRequestUrl(String apiKeyValue) {
        return attachGatewayIdToUrl(apiKeyRequestUrlWithoutApiKeyValueAndGatewayId + apiKeyValue);
    }

    public String attachGatewayIdToUrl(String urlWithoutApiKey) {
        String url = urlWithoutApiKey;

        if (urlWithoutApiKey.contains("?")) {
            url += "&";
        } else {
            url += "?";
        }
        url += Proxy.GATEWAY_API_KEY_REQUEST_PARAMETER_NAME + "=" + gatewayId;

        return url;
    }

    public String getApiKeyListRequestUrl() {
        return apiKeyListRequestUrl;
    }

    public String getGatewayId() {
        return gatewayId;
    }
}
