package eu.coatrack.admin.service;

import eu.coatrack.admin.controllers.AdminServicesController;
import eu.coatrack.admin.model.repository.CoverRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.api.ServiceApi;
import eu.coatrack.api.ServiceCover;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.UUID;

@Service
public class CoverService {


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

    public ModelAndView fileUpload(Authentication auth, Long serviceId, MultipartFile file) throws IOException, java.text.ParseException {
        if(!file.isEmpty()) {
            ServiceApi service = serviceRepository.findById(serviceId).orElse(null);

            String coverFilename = UUID.randomUUID().toString();

            ServiceCover cover = new ServiceCover();
            cover.setService(service);
            cover.setOriginalFileName(StringUtils.cleanPath(file.getOriginalFilename()));
            cover.setFileName(coverFilename);
            cover.setFileType(file.getContentType());
            cover.setSize(file.getSize());
            cover.setLocalPath(serviceCoversPath + File.separator + coverFilename);
            cover.setUrl(serviceCoversUrl + coverFilename);

            coverImageReadService.readExcelInputStream(new ByteArrayInputStream(file.getBytes()), new File(cover.getLocalPath()));
            coverRepository.save(cover);
        }
        return adminServicesController.serviceListPage();
    }
}
