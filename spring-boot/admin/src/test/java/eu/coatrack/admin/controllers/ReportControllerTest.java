package eu.coatrack.admin.controllers;

import eu.coatrack.admin.model.repository.ApiKeyRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.admin.service.report.ReportService;
import eu.coatrack.api.ApiUsageReport;
import eu.coatrack.api.DataTableView;
import eu.coatrack.api.ServiceApi;
import eu.coatrack.api.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

import static eu.coatrack.admin.factories.ReportDataFactory.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.ModelAndViewAssert.assertCompareListModelAttribute;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// I don't actually know why ServiceApiRepository or UserRepository are sufficient to declare context
@ContextConfiguration(classes = {ServiceApiRepository.class})
@WebMvcTest(ReportController.class)
public class ReportControllerTest {

    private final MockMvc mvc;

    private final UserRepository userRepository;
    private final ServiceApiRepository serviceApiRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ReportService reportService;

    private final ReportController reportController;


    public ReportControllerTest() {
        userRepository = mock(UserRepository.class);
        serviceApiRepository = mock(ServiceApiRepository.class);
        apiKeyRepository = mock(ApiKeyRepository.class);
        reportService = mock(ReportService.class);

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ADMIN");
        Authentication authentication = new UsernamePasswordAuthenticationToken(getConsumer().getUsername(), "PetesPassword", Collections.singletonList(authority));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        reportController = new ReportController(userRepository, serviceApiRepository, apiKeyRepository, reportService);
        mvc = MockMvcBuilders.standaloneSetup(reportController)
                .build();
    }

    @Test
    public void reportWithoutParam() throws Exception {
        doReturn(getConsumer()).when(userRepository).findByUsername(anyString());
        doReturn(getServiceList()).when(serviceApiRepository).findByDeletedWhen(null);
        doReturn(getConsumers()).when(reportService).getServiceConsumers(anyList());
        doReturn(Arrays.asList("1", "2", "3")).when(reportService).getPayPerCallServicesIds(anyList());

        mvc.perform(get("/admin/reports/"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(ReportController.REPORT_VIEW))
                .andExpect(model().attribute("users", ))
                .andExpect(model().attribute("selectedServiceId",))
                .andExpect(model().attribute("selectedApiConsumerUserId", ))
                .andExpect(model().attribute("services", ))
                .andExpect(model().attribute("payPerCallServicesIds", ))
                .andExpect(model().attribute("exportUser", ))
                .andExpect(model().attribute("isOnlyPaidCalls", )) // TODO delete
                .andExpect(model().attribute("isReportForConsumer", )) // TODO delete
                .andExpect(model().attribute("dateFrom", )) // TODO delete
                .andExpect(model().attribute("dateUntil", )) // TODO delete
                .andExpect(model().attribute("serviceApiSelectedForReport", )) // TODO delete
                .andExpect(model().attribute("consumerUserSelectedForReport", )); // TODO delete


    }


    @Test
    public void reportWithParam() {
        String dateFrom = "22-06-2022";
        String dateUntil = "28-06-2022";
        long selectedServiceId = 0L;
        long selectedApiConsumerUserId = 0L;
        boolean considerOnlyPaidCalls = false;

    }

    @Test
    public void reportApiUsage() {
        String dateFrom = "";
        String dateUntil = "";
        long selectedServiceId = 0L;
        long selectedApiConsumerUserId = 0L;
        boolean considerOnlyPaidCalls = false;

    }

    @Test
    public void showGenerateReportPageForServiceConsumer() {


    }

    @Test
    public void searchReportsByServicesConsumed() {
        String dateFrom = "";
        String dateUntil = "";
        long selectedServiceId = 0L;
        boolean considerOnlyPaidCalls = false;


    }

}
