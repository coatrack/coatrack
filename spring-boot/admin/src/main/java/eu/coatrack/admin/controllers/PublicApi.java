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

import eu.coatrack.admin.logic.CreateApiKeyAction;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.api.*;

import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.modelmapper.convention.MatchingStrategies.STRICT;

@RestController
@RequestMapping(value = "/public-api")
@Component
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class PublicApi implements ServiceApiService, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(PublicApi.class);

    @Autowired
    private ServiceApiRepository serviceApiRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CreateApiKeyAction createApiKeyAction;

    @Autowired
    private ReportController reportController;

    private ModelMapper modelMapper;

    public void afterPropertiesSet() throws Exception {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(STRICT);
        modelMapper.getConfiguration().setPropertyCondition(Conditions.isNotNull());
    }

    @GetMapping(value = "/services/{id}", produces = "application/json")
    @Override
    public ServiceApiDTO findById(@PathVariable("id") Long id) {
        return toDTO(serviceApiRepository.findOne(id));
    }

    @GetMapping(value = "/services/{id}.csv", produces = "text/csv")
    public void findByIdCSV(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        ServiceApiDTO serviceApiDTO = toDTO(serviceApiRepository.findOne(id));
        ICsvBeanWriter csvWriter = new CsvBeanWriter(response.getWriter(), CsvPreference.STANDARD_PREFERENCE);
        csvWriter.write(serviceApiDTO);
        csvWriter.close();
    }

    @GetMapping(value = "/services", produces = "application/json")
    public List<ServiceApiDTO> findByServiceOwner() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return toListDTO(serviceApiRepository.findByOwnerUsername(auth.getName()));
    }

    @GetMapping(value = "/services.csv", produces = "text/csv")
    public void findAllCSV(HttpServletResponse response) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Iterable<ServiceApi> serviceApis = serviceApiRepository.findByOwnerUsername(auth.getName());
        ICsvBeanWriter csvWriter = new CsvBeanWriter(response.getWriter(), CsvPreference.STANDARD_PREFERENCE);

        List<String> names = new ArrayList();
        Field[] allFields = ServiceApiDTO.class.getDeclaredFields();
        for (Field field : allFields) {
            names.add(field.getName().toString());
        }
        String[] header = names.toArray(new String[names.size()]);

        csvWriter.writeHeader(header);
        for (ServiceApi item : serviceApis) {
            ServiceApiDTO metricDTO = toDTO(item);
            csvWriter.write(metricDTO, header);
        }
        csvWriter.close();
    }

    @PostMapping(value = "services/{id}/subscriptions", produces = "application/json")
    public String subscribeService(@PathVariable("id") Long id) throws UnknownHostException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User userWhoSubscribes = userRepository.findByUsername(auth.getName());
        ServiceApi serviceToSubscribeTo = serviceApiRepository.findOne(id);
        createApiKeyAction.setServiceApi(Service);
        createApiKeyAction.setUser(user);
        createApiKeyAction.execute();
        String baseURL = "http://" + Inet4Address.getLocalHost().getHostAddress() + ":8080/admin/api-keys";
        return baseURL;
    }

    @GetMapping(value = "services/{id}/usageStatistics")
    public long getServiceUsageStatistics(@PathVariable("id") Long id, @RequestParam String dateFrom, @RequestParam String dateUntil) throws IOException, ParseException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ServiceApi service = serviceApiRepository.findOne(id);
        Long userId = userRepository.findByUsername(auth.getName()).getId();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date dateFromParsedToDate = formatter.parse(dateFrom);
        Date dateUntilParsedToDate = formatter.parse(dateUntil);
        List<ApiUsageReport> apiUsageReports = new ArrayList<>();
        // if user is the owner of the service
        if (service.getOwner().getUsername().equals(auth.getName())) {

            apiUsageReports.addAll(reportController.calculateApiUsageReportForSpecificService(service, -1L, // for all consumers
                    java.sql.Date.valueOf(dateFromParsedToDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()),
                    java.sql.Date.valueOf(dateUntilParsedToDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()),
                    true));
        } else {
            apiUsageReports = reportController.calculateApiUsageReportForSpecificService(service, userId, dateFromParsedToDate, dateUntilParsedToDate, false);
        }
        return apiUsageReports.get(0).getCalls();
    }

    List<ServiceApiDTO> toListOfDTOs(List<ServiceApi> entity) {
        List<ServiceApiDTO> serviceApiDTOList = new ArrayList<>();
        for (ServiceApi singleEntity : entity) {
            serviceApiDTOList.add(modelMapper.map(singleEntity, ServiceApiDTO.class));
        }
        return serviceApiDTOList;
    }

    ServiceApiDTO toDTO(ServiceApi entity) {
        return modelMapper.map(entity, ServiceApiDTO.class);
    }

    ServiceApi toEntity(ServiceApiDTO dto) {
        return modelMapper.map(dto, ServiceApi.class);
    }

}