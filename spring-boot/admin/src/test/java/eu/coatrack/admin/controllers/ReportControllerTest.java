package eu.coatrack.admin.controllers;

import eu.coatrack.admin.YggAdminApplication;
import eu.coatrack.admin.model.repository.ApiKeyRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.admin.service.report.ReportService;
import eu.coatrack.api.ApiUsageReport;
import eu.coatrack.api.DataTableView;
import eu.coatrack.api.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static eu.coatrack.admin.service.report.ReportDataFactory.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(classes = {YggAdminApplication.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ReportControllerTest {
    private ReportController reportController;
    @Autowired
    private MockMvc mvc;

    private UserRepository userRepository;
    private ServiceApiRepository serviceApiRepository;
    private ApiKeyRepository apiKeyRepository;
    private ReportService reportService;

    public ReportControllerTest() {
        userRepository = mock(UserRepository.class);
        serviceApiRepository = mock(ServiceApiRepository.class);
        apiKeyRepository = mock(ApiKeyRepository.class);
        reportService = mock(ReportService.class);

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("Tester");
        Authentication authentication = new UsernamePasswordAuthenticationToken(getConsumer().getUsername(), "PetesPassword", Collections.singletonList(authority));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        reportController = new ReportController(userRepository, serviceApiRepository, apiKeyRepository, reportService);
    }

    @Test
    public void reportWithoutParam() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                .get("/admin/reports"))
                .andExpect(model().size(12))
                .andExpect(status().isOk());

    }


    @Test
    public void reportWithParam() {
        String dateFrom = "22-06-2022";
        String dateUntil = "28-06-2022";
        long selectedServiceId = 0L;
        long selectedApiConsumerUserId = 0L;
        boolean considerOnlyPaidCalls = false;


        doReturn(getConsumer()).when(userRepository).findByUsername(getConsumer().getUsername());
        doReturn(getServiceList()).when(serviceApiRepository).findByDeletedWhen(null);
        doReturn(getConsumers()).when(reportService).getServiceConsumers(anyList());
        doReturn(Arrays.asList(1L, 2L, 3L)).when(reportService).getPayPerCallServicesIds(anyList());




    }

    @Test
    public void reportApiUsage() {
        String dateFrom = "";
        String dateUntil = "";
        long selectedServiceId = 0L;
        long selectedApiConsumerUserId = 0L;
        boolean considerOnlyPaidCalls = false;

        DataTableView<ApiUsageReport> actual = reportController.reportApiUsage(dateFrom, dateUntil, selectedServiceId, selectedApiConsumerUserId, considerOnlyPaidCalls);


        // TODO can I remove the DataTableView from the Project? It seems to be just a wrapper for a list
        DataTableView<ApiUsageReport> expected = new DataTableView<>();

    }

    @Test
    public void showGenerateReportPageForServiceConsumer() {
        ModelAndView actual = reportController.showGenerateReportPageForServiceConsumer();

        ModelAndView expected = new ModelAndView();

    }

    @Test
    public void searchReportsByServicesConsumed() {
        String dateFrom = "";
        String dateUntil = "";
        long selectedServiceId = 0L;
        boolean considerOnlyPaidCalls = false;

        ModelAndView actual = reportController.searchReportsByServicesConsumed(dateFrom, dateUntil, selectedServiceId, considerOnlyPaidCalls);

        ModelAndView expected = new ModelAndView();

    }

}
