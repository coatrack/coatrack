package eu.coatrack.admin.controllers;

/*-
 * #%L
 * coatrack-admin
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.UUID;
import eu.coatrack.admin.model.repository.CoverRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.service.CoverImageReadService;
import eu.coatrack.api.ServiceApi;
import eu.coatrack.api.ServiceCover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/admin/covers")
public class CoverController {

    private static final Logger log = LoggerFactory.getLogger(CoverController.class);

    @Autowired
    private CoverRepository coverRepository;

    @Autowired
    private ServiceApiRepository serviceRepository;

    @Autowired
    private CoverImageReadService coverImageReadService;

    @Autowired
    AdminServicesController adminServicesController;

    @Value("${ygg.admin.servicecovers.path}")
    private String serviceCoversPath;

    @Value("${ygg.admin.servicecovers.url}")
    private String serviceCoversUrl;

    @RequestMapping(value = "/{id}/upload")
    public ModelAndView fileUpload(Authentication auth, @PathVariable("id") Long serviceId, @RequestParam("file") MultipartFile file) throws IOException, ParseException, java.text.ParseException {

        ServiceApi service = serviceRepository.findOne(serviceId);

        String coverFilename = UUID.randomUUID().toString();

        ServiceCover cover = new ServiceCover();
        cover.setService(service);
        cover.setOriginalFileName(StringUtils.cleanPath(file.getOriginalFilename()));
        cover.setFileName(coverFilename);
        cover.setFileType(file.getContentType());
        cover.setSize(file.getSize());
        cover.setLocalPath(serviceCoversPath + File.separator + coverFilename);
        cover.setUrl(serviceCoversUrl + coverFilename);

        coverImageReadService.readExcelInputStream(new ByteArrayInputStream(file.getBytes()),
                new File(cover.getLocalPath()));

        coverRepository.save(cover);

        return adminServicesController.serviceListPage();
    }
}
