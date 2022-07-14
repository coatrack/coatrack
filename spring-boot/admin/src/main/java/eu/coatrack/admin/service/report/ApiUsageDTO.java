package eu.coatrack.admin.service.report;

import eu.coatrack.api.ServiceApi;
import eu.coatrack.api.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@Getter
@Setter
public class ApiUsageDTO {
        private final ServiceApi service;
        private final User consumer;
        private final Date from;
        private final Date until;
        private final boolean considerOnlyPaidCalls;
        private final boolean isForConsumer;
}
