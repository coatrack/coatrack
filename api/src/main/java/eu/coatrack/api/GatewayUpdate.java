package eu.coatrack.api;

/*-
 * #%L
 * coatrack-api
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

import java.sql.Timestamp;

/**
 * This is a wrapper class which contains update content for the gateways.
 *
 * @author = Christoph Baier
 */

public class GatewayUpdate {

    public ApiKey[] apiKeys;
    public ServiceApi[] serviceApis;
    public Timestamp adminsLocalTime;

    public GatewayUpdate(){
        super();
    }

    public GatewayUpdate(ApiKey[] apiKeys, ServiceApi[] serviceApis, Timestamp adminsLocalTime){
        this.apiKeys = apiKeys;
        this.serviceApis = serviceApis;
        this.adminsLocalTime = adminsLocalTime;
    }

}
