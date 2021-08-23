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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import java.util.logging.Logger;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;

/**
 * @author gr-hovest
 */
@Entity
@Table(name = "api_keys")
public class ApiKey {

    public static final String API_KEY_FOR_YGG_ADMIN_TO_ACCESS_PROXIES = "6716d109900b9055c2bc46994D29618C9DC888AE305C45F34013";
    public static final String API_KEY_REQUEST_PARAMETER_NAME = "api-key";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String keyValue;

    @Temporal(javax.persistence.TemporalType.DATE)
    private Date created;

    @Temporal(javax.persistence.TemporalType.DATE)
    private Date validUntil;

    @OneToOne
    private ServiceApi serviceApi;

    @OneToOne
    private User user;

    @Temporal(javax.persistence.TemporalType.DATE)
    private Date deletedWhen;

    public User getUser() {
        return user;
    }

    public void setUser(User owner) {
        this.user = owner;
    }

    public Date getDeletedWhen() {
        return deletedWhen;
    }

    public void setDeletedWhen(Date deletedWhen) {
        this.deletedWhen = deletedWhen;
    }

    public String getUserName() {
        String username = "";
        if (user != null) {
            username = user.username;
        }
        return username;
    }

    public ServiceApi getServiceApi() {
        return serviceApi;
    }

    public String getServiceApiName() {

        String serviceApiName = "";
        if (serviceApi != null) {
            serviceApiName = serviceApi.getName();
        }
        return serviceApiName;
    }

    public void setServiceApi(ServiceApi serviceApi) {
        this.serviceApi = serviceApi;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Date validUntil) {
        this.validUntil = validUntil;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private static final Logger LOG = Logger.getLogger(ApiKey.class.getName());

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ApiKey{" + "id=" + id + ", keyValue='" + keyValue + '\'' + ", created=" + created + ", validUntil="
                + validUntil + ", serviceApi=" + serviceApi + ", user=" + user + '}';
    }
}
