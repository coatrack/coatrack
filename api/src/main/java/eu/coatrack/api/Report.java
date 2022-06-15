package eu.coatrack.api;

import java.util.Date;

public class Report {
    private boolean isOnlyPaidCalls;
    private boolean isForConsumer;
    private Date from;
    private Date until;
    private ServiceApi selectedService;
    private User selectedConsumer;

    public Report(boolean isOnlyPaidCalls, boolean isForConsumer, Date from, Date until, ServiceApi selectedService, User selectedConsumer) {
        this.isOnlyPaidCalls = isOnlyPaidCalls;
        this.isForConsumer = isForConsumer;
        this.from = from;
        this.until = until;
        this.selectedService = selectedService;
        this.selectedConsumer = selectedConsumer;
    }

    public Report() {}

    public Report setOnlyPaidCalls(boolean onlyPaidCalls) {
        isOnlyPaidCalls = onlyPaidCalls;
        return this;
    }

    public Report setForConsumer(boolean forConsumer) {
        isForConsumer = forConsumer;
        return this;
    }

    public Report setFrom(Date from) {
        this.from = from;
        return this;
    }

    public Report setUntil(Date until) {
        this.until = until;
        return this;
    }

    public Report setSelectedService(ServiceApi selectedService) {
        this.selectedService = selectedService;
        return this;
    }

    public Report setSelectedConsumer(User selectedConsumer) {
        this.selectedConsumer = selectedConsumer;
        return this;
    }

    public boolean isOnlyPaidCalls() {
        return isOnlyPaidCalls;
    }

    public boolean isForConsumer() {
        return isForConsumer;
    }

    public Date getFrom() {
        return from;
    }

    public Date getUntil() {
        return until;
    }

    public ServiceApi getSelectedService() {
        return selectedService;
    }

    public User getSelectedConsumer() {
        return selectedConsumer;
    }
}
