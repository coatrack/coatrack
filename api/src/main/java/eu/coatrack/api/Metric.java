package eu.coatrack.api;

/*-
 * #%L
 * ygg-api
 * %%
 * Copyright (C) 2013 - 2019 Corizon
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

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Date;
import javax.validation.constraints.NotNull;

/**
 * @author monezz
 */
@Entity
@Table(name = "metrices")
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "proxy_id")
    @JsonIgnore
    private Proxy proxy;

    @Enumerated(EnumType.STRING)
    private MetricType type;

    private String requestMethod = "";
    private String path = "";
    private Integer httpResponseCode;
    private int count;
    private String metricsCounterSessionID = "";

    @Temporal(TemporalType.DATE)
    private Date dateOfApiCall;

    @OneToOne
    @NotNull
    private ApiKey apiKey;

    public long getId() {
        return id;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    public void setApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public MetricType getType() {
        return type;
    }

    public void setType(MetricType type) {
        this.type = type;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getHttpResponseCode() {
        return httpResponseCode;
    }

    public void setHttpResponseCode(Integer httpResponseCode) {
        this.httpResponseCode = httpResponseCode;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getMetricsCounterSessionID() {
        return metricsCounterSessionID;
    }

    public void setMetricsCounterSessionID(String metricsCounterSessionID) {
        this.metricsCounterSessionID = metricsCounterSessionID;
    }

    public Date getDateOfApiCall() {
        return dateOfApiCall;
    }

    public void setDateOfApiCall(Date dateOfApiCall) {
        this.dateOfApiCall = dateOfApiCall;
    }

    @Override
    public String toString() {
        return "Metric{"
                + "id=" + id
                + ", proxy=" + proxy
                + ", type=" + type
                + ", requestMethod='" + requestMethod + '\''
                + ", path='" + path + '\''
                + ", httpResponseCode=" + httpResponseCode
                + ", count=" + count
                + ", metricsCounterSessionID='" + metricsCounterSessionID + '\''
                + ", dateOfApiCall=" + dateOfApiCall
                + '}';
    }
}
