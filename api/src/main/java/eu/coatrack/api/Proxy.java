package eu.coatrack.api;

/*-
 * #%L
 * coatrack-api
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

import javax.persistence.*;
import java.util.*;

/**
 *
 * @author monezz
 */
@Entity
@Table(name = "gateways")
public class Proxy {

    public static final String GATEWAY_API_KEY_REQUEST_PARAMETER_NAME = "gateway-api-key";

    public Proxy() {
    }

    public Proxy(String id) {
        this.id = id;
    }

    @Id
    private String id;

    private String name;

    private String description;

    private String publicUrl;

    private Integer port;

    private String configServerName;

    private String configServerPassword;

    @OneToOne
    private User owner;

    @OneToMany(mappedBy = "proxy", cascade = CascadeType.ALL)
    private List<Metric> metrics = new ArrayList<>(0);

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<ServiceApi> serviceApis = new HashSet<>(0);

    @Temporal(javax.persistence.TemporalType.DATE)
    private Date deletedWhen;

    public String getCredentialName() {
        return configServerName;
    }

    public void setConfigServerName(String credential_id) {
        this.configServerName = credential_id;
    }

    public String getConfigServerPassword() {
        return configServerPassword;
    }

    public void setConfigServerPassword(String credential_password) {
        this.configServerPassword = credential_password;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Date getDeletedWhen() {
        return deletedWhen;
    }

    public void setDeletedWhen(Date deletedWhen) {
        this.deletedWhen = deletedWhen;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<ServiceApi> getServiceApis() {
        return serviceApis;
    }

    public void setServiceApis(Set<ServiceApi> serviceApis) {
        this.serviceApis = serviceApis;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "Proxy{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", publicUrl='" + publicUrl + '\''
                + ", port='" + port + '\'' + '}';
    }
}
