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
import java.util.*;

/**
 *
 * @author monezz
 */
@Entity
@Table(name = "covers")
public class ServiceCover {

    public ServiceCover() {
    }

    public ServiceCover(String id) {
        this.id = id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    private String fileName;

    private String originalFileName;

    private String fileType;

    private String url;

    @JsonIgnore
    private String localPath;

    @Column(nullable = true)
    private long size;

    @OneToOne
    private ServiceApi service;

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Temporal(javax.persistence.TemporalType.DATE)
    private Date deletedWhen;

    public Date getDeletedWhen() {
        return deletedWhen;
    }

    public void setDeletedWhen(Date deletedWhen) {
        this.deletedWhen = deletedWhen;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public ServiceApi getService() {
        return service;
    }

    public void setService(ServiceApi service) {
        this.service = service;
    }

}
