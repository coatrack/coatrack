package eu.coatrack.admin.service;

import eu.coatrack.admin.components.WebUI;
import eu.coatrack.admin.model.repository.CoverRepository;
import eu.coatrack.admin.model.repository.ErrorRepository;
import eu.coatrack.api.ServiceCover;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.SpringVersion;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Service
public class HomeService {
    public static final String HOME_VIEW = "home";
    public static final String ERROR_VIEW = "error";
    public static final String REDIRECT_HOME_VIEW = "redirect:/";
    public static final String ERROR_403_VIEW = "errors/custom";

    private final WebUI webUI;

    @Autowired
    private ErrorRepository errorRepository;

    @Autowired
    private CoverRepository coverRepository;

    @Autowired
    public HomeService(WebUI webUI) {
        this.webUI = webUI;
    }

    public String home(Model model) {
        String springVersion = webUI.parameterizedMessage("home.spring.version", SpringBootVersion.getVersion(), SpringVersion.getVersion());
        model.addAttribute("springVersion", springVersion);
        return HOME_VIEW;
    }

    public eu.coatrack.api.Error saveErrors(eu.coatrack.api.Error error) {
        return errorRepository.save(error);
    }

    public Iterable<ServiceCover> serviceCoversListPageRest(){
        return coverRepository.findAll();
    }
}
