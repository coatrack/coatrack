package eu.coatrack.admin.service.report;

import eu.coatrack.api.ServiceApi;
import eu.coatrack.api.User;
import lombok.AllArgsConstructor;

import java.util.Date;

@AllArgsConstructor
public class ApiUsageDTO {
        public final ServiceApi service;
        public final User consumer;
        public final Date from;
        public final Date until;
        public final boolean considerOnlyPaidCalls;
        public final boolean isForConsumer;
}
