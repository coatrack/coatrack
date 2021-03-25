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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author gr-hovest
 */
@Entity
@Table(name = "service_apis")
public class ServiceApi implements ServiceApiInterface{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String uriIdentifier;

    @Column(nullable = false)
    private String localUrl;

    private double monthlyFee;

    @Enumerated(EnumType.STRING)
    private ServiceAccessPermissionPolicy serviceAccessPermissionPolicy;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<ApiKey> apiKeys = new ArrayList<>(0);

    @Enumerated(EnumType.STRING)
    private ServiceAccessPaymentPolicy serviceAccessPaymentPolicy;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("position")
    private List<EntryPoint> entryPoints = new ArrayList<>(0);

    @OneToOne
    @JsonIgnore
    private User owner;

    @Temporal(javax.persistence.TemporalType.DATE)
    private Date deletedWhen;

    /*@OneToOne(optional = true)
    private ServiceCover cover;

    public ServiceCover getCover() {
        return cover;
    }

    public void setCover(ServiceCover cover) {
        this.cover = cover;
    }*/

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public double getMonthlyFee() {
        return monthlyFee;
    }

    public void setMonthlyFee(double monthlyFee) {
        this.monthlyFee = monthlyFee;
    }

    public Date getDeletedWhen() {
        return deletedWhen;
    }

    public void setDeletedWhen(Date deletedWhen) {
        this.deletedWhen = deletedWhen;
    }

    public List<ApiKey> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(List<ApiKey> apiKey) {
        this.apiKeys = apiKey;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    /**
     *
     * @return the name to identify this ServiceApi in the admin GUI
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return a description of this service API, its intended purpose and use
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String getServiceOwnerUsername() {
        return owner.getUsername();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     * @return the part of the service gateway URI that identifies this service
     * API
     */
    public String getUriIdentifier() {
        return uriIdentifier;
    }

    public void setUriIdentifier(String uriIdentifier) {
        this.uriIdentifier = uriIdentifier;
    }

    /**
     *
     * @return the URL in the local area network of the service API provider
     * where the service can be reached via the service gateway
     */
    public String getLocalUrl() {
        return localUrl;
    }

    public void setLocalUrl(String localUrl) {
        this.localUrl = localUrl;
    }

    public ServiceAccessPermissionPolicy getServiceAccessPermissionPolicy() {
        return serviceAccessPermissionPolicy;
    }

    public void setServiceAccessPermissionPolicy(ServiceAccessPermissionPolicy serviceAccessPermissionPolicy) {
        this.serviceAccessPermissionPolicy = serviceAccessPermissionPolicy;
    }

    public ServiceAccessPaymentPolicy getServiceAccessPaymentPolicy() {
        return serviceAccessPaymentPolicy;
    }

    public void setServiceAccessPaymentPolicy(ServiceAccessPaymentPolicy serviceAccessPaymentPolicy) {
        this.serviceAccessPaymentPolicy = serviceAccessPaymentPolicy;
    }

    public List<EntryPoint> getEntryPoints() {
        return entryPoints;
    }

    public void setEntryPoints(List<EntryPoint> entryPoints) {
        this.entryPoints = entryPoints;
    }

    /**
     *
     * @return a simple String to display the Service API in a GUI
     */
    public String getGuiStringRepresentation() {
        return String.format("%s (%s - %s)", name, uriIdentifier, localUrl);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + (int) (this.id ^ (this.id >>> 32));
        hash = 37 * hash + Objects.hashCode(this.name);
        hash = 37 * hash + Objects.hashCode(this.description);
        hash = 37 * hash + Objects.hashCode(this.uriIdentifier);
        hash = 37 * hash + Objects.hashCode(this.localUrl);
        hash = 37 * hash + Objects.hashCode(this.serviceAccessPermissionPolicy);
        hash = 37 * hash + Objects.hashCode(this.apiKeys);
        hash = 37 * hash + Objects.hashCode(this.serviceAccessPaymentPolicy);
        hash = 37 * hash + Objects.hashCode(this.entryPoints);
        hash = 37 * hash + Objects.hashCode(this.owner);
        hash = 37 * hash + Objects.hashCode(this.deletedWhen);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ServiceApi other = (ServiceApi) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ServiceApi{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", uriIdentifier='" + uriIdentifier + '\''
                + ", localUrl='" + localUrl + '\''
                + ", user=" + owner
                + '}';
    }
}
