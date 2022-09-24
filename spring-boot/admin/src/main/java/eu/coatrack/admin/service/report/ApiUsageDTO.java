package eu.coatrack.admin.service.report;

import eu.coatrack.api.ServiceApi;
import eu.coatrack.api.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ApiUsageDTO {
        private ServiceApi service;
        private User consumer;
        private Date from;
        private Date until;
        private boolean considerOnlyPaidCalls;
        private boolean isForConsumer;
}
