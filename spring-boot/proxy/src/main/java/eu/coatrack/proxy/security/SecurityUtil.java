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

@Service("securityUtil")
public class SecurityUtil {

    @Value("${proxy-id}")
    private String myProxyID;

    public String attachGatewayApiKeyToUrl(String urlWithoutApiKey) {

        String url = urlWithoutApiKey;
        if (!urlWithoutApiKey.contains("?")) {
            url += "?";
        } else {
            url += "&";
        }
        url += Proxy.GATEWAY_API_KEY_REQUEST_PARAMETER_NAME + "=" + myProxyID;

        return url;
    }
}
