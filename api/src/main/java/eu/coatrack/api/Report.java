package eu.coatrack.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@Getter
@Setter
public class Report {
    private boolean isOnlyPaidCalls;
    private boolean isForConsumer;
    private Date from;
    private Date until;
    private ServiceApi selectedService;
    private User selectedConsumer;
}
