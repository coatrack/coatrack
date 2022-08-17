package eu.coatrack.admin.controllers;

import eu.coatrack.admin.model.repository.ApiKeyRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.admin.service.report.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;

import static eu.coatrack.admin.factories.ReportDataFactory.getConsumer;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ContextConfiguration(classes = {UserRepository.class, ServiceApiRepository.class, ApiKeyRepository.class, ReportService.class, ReportController.class})
@WebMvcTest(controllers = {ReportController.class})
public class ReportControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ServiceApiRepository serviceApiRepository;

    @MockBean
    private ApiKeyRepository apiKeyRepository;

    @MockBean
    private ReportService reportService;


    //private ReportController reportController;


    public ReportControllerTest() {
        /*userRepository = mock(UserRepository.class);
        serviceApiRepository = mock(ServiceApiRepository.class);
        apiKeyRepository = mock(ApiKeyRepository.class);
        reportService = mock(ReportService.class);*/

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("Tester");
        Authentication authentication = new UsernamePasswordAuthenticationToken(getConsumer().getUsername(), "PetesPassword", Collections.singletonList(authority));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        //reportController = new ReportController(userRepository, serviceApiRepository, apiKeyRepository, reportService);
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
/*        String dateFrom = "22-06-2022";
        String dateUntil = "28-06-2022";
        long selectedServiceId = 0L;
        long selectedApiConsumerUserId = 0L;
        boolean considerOnlyPaidCalls = false;


        doReturn(getConsumer()).when(userRepository).findByUsername(getConsumer().getUsername());
        doReturn(getServiceList()).when(serviceApiRepository).findByDeletedWhen(null);
        doReturn(getConsumers()).when(reportService).getServiceConsumers(anyList());
        doReturn(Arrays.asList(1L, 2L, 3L)).when(reportService).getPayPerCallServicesIds(anyList());*/




    }

    @Test
    public void reportApiUsage() {
        /*String dateFrom = "";
        String dateUntil = "";
        long selectedServiceId = 0L;
        long selectedApiConsumerUserId = 0L;
        boolean considerOnlyPaidCalls = false;

        DataTableView<ApiUsageReport> actual = reportController.reportApiUsage(dateFrom, dateUntil, selectedServiceId, selectedApiConsumerUserId, considerOnlyPaidCalls);


        // TODO can I remove the DataTableView from the Project? It seems to be just a wrapper for a list
        DataTableView<ApiUsageReport> expected = new DataTableView<>();*/

    }

    @Test
    public void showGenerateReportPageForServiceConsumer() {
       /* ModelAndView actual = reportController.showGenerateReportPageForServiceConsumer();

        ModelAndView expected = new ModelAndView();*/

    }

    @Test
    public void searchReportsByServicesConsumed() {
        /*String dateFrom = "";
        String dateUntil = "";
        long selectedServiceId = 0L;
        boolean considerOnlyPaidCalls = false;

        ModelAndView actual = reportController.searchReportsByServicesConsumed(dateFrom, dateUntil, selectedServiceId, considerOnlyPaidCalls);

        ModelAndView expected = new ModelAndView();*/

    }

}
