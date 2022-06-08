package eu.coatrack.admin.service;

import eu.coatrack.admin.model.repository.*;
import eu.coatrack.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class MetricsService {

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private final AntPathMatcher parser = new AntPathMatcher();

    public Long storeMetricAndProperlySetRelationships(String proxyId, String apiKeyValue, Metric metricSubmitted) {
        Metric metricStored = new Metric();
        log.debug("metric {} received from proxy {} with api key {}", metricSubmitted, proxyId, apiKeyValue);

        // the proxy and apiKey entities are NOT included in metricSubmitted, therefore IDs sent separately
        Proxy proxy = proxyRepository.findById(proxyId).orElse(null);

        if (proxy != null ) {
            proxy.updateTimeOfLastSuccessfulCallToAdmin_setToNow();
            ApiKey apiKey = apiKeyRepository.findByKeyValue(apiKeyValue);
            if(apiKey != null) {
                metricStored = saveMetric(proxy, apiKey, metricSubmitted);

                 /* The proxy calls CoatRack admin two times:
              1 - once for the request to the gateway
              2 - once for the response from the provider service
             Only case (2) is relevant for the credit transfer.
             In addition, the response code should be checked for number "< 400", because 4xx codes refer to client side error while the 5xx codes are related to server errors
             So only http response codes indicating successful calls should lead to a credit transfer */
                if (metricSubmitted.getHttpResponseCode() != null && metricSubmitted.getHttpResponseCode() < 400)
                    resolveCreditTransfer(apiKey, proxy, metricSubmitted);
            }
        }
        return metricStored.getId();
    }

    private Metric saveMetric(Proxy proxy, ApiKey apiKey, Metric metricSubmitted) {
        Metric metricToBeStored;

        // check if there is already a metric entry in the database, which can be updated instead of storing a new entry
        List<Metric> metricToBeUpdated = metricRepository.findByProxyAndTypeAndRequestMethodAndPathAndHttpResponseCodeAndMetricsCounterSessionIDAndDateOfApiCallAndApiKeyOrderByIdDesc(
                proxy,
                metricSubmitted.getType(),
                metricSubmitted.getRequestMethod(),
                metricSubmitted.getPath(),
                metricSubmitted.getHttpResponseCode(),
                metricSubmitted.getMetricsCounterSessionID(),
                metricSubmitted.getDateOfApiCall(),
                apiKey
        );

        if (metricToBeUpdated == null || metricToBeUpdated.isEmpty()) {
            // there is no metric to be updated, just store the submitted metric in DB
            metricToBeStored = metricSubmitted;
            log.debug("no metric found in DB, so the submitted one will be stored: {}", metricToBeStored);
        } else {
            // metric row in DB should be updated, instead of storing a new row
            if (metricToBeUpdated.size() > 1) {
                // this should usually not happen, however some historic data in DB could lead to this situation
                log.warn("More than one metric found that should be updated: {}", metricToBeUpdated.size());
            }
            metricToBeStored = metricToBeUpdated.get(0);
            metricToBeStored.setCount(metricSubmitted.getCount());
            log.debug("metric found in DB, will be updated: {}", metricToBeStored);
        }

        log.debug("store metric {} properly for proxy {} and api key {}", metricToBeStored, proxy.getId(), apiKey.getKeyValue());
        metricToBeStored.setProxy(proxy);
        metricToBeStored.setApiKey(apiKey);
        metricToBeStored = metricRepository.save(metricToBeStored);

        return metricToBeStored;
    }

    private void resolveCreditTransfer(ApiKey apiKey, Proxy proxy, Metric metricSubmitted) {
        log.debug("credit transfer from consumer to provider");
        ServiceApi service = apiKey.getServiceApi();
        if (service != null) {
            // Monthly Fee will be processed indepently in a cron thread
            if (service.getServiceAccessPaymentPolicy().equals(ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE)) {

                User provider = proxy.getOwner();
                User consumer = apiKey.getUser();

                // Create a new transaction
                Transaction providerTransaction = createTransaction(TransactionType.SERVICE_DEPOSIT, provider);
                Transaction consumerTransaction = createTransaction(TransactionType.FEE, consumer);

                // Calculate cost
                BigDecimal price = new BigDecimal(0);
                for (EntryPoint entryPoint : service.getEntryPoints()) {
                    if (entryPoint.getPathPattern() != null && entryPoint.getHttpMethod() != null) {

                        boolean pathsMatches = parser.match(entryPoint.getPathPattern(), metricSubmitted.getPath());

                        if (pathsMatches) {
                            if (entryPoint.getHttpMethod().equals(metricSubmitted.getRequestMethod()) || entryPoint.getHttpMethod().equals("*")) {
                                price = price.add(BigDecimal.valueOf(entryPoint.getPricePerCall() / 1000));

                                addTransactionDetails(consumerTransaction, price);
                                addTransactionDetails(providerTransaction, price);

                                log.debug("Transferring {} EUR from {} to {}", price.doubleValue(), consumer.getUsername(), provider.getUsername());

                                consumer.getAccount().setBalance(consumer.getAccount().getBalance() - price.doubleValue());
                                provider.getAccount().setBalance(provider.getAccount().getBalance() + price.doubleValue());

                            }
                        }
                    }
                }
                userRepository.save(provider);
                userRepository.save(consumer);
            }
        }
    }

    private void addTransactionDetails(Transaction transactionDetails, BigDecimal price) {
        transactionDetails.setAmount(price.doubleValue());
        transactionRepository.save(transactionDetails);
        transactionDetails.getAccount().getTransaction().add(transactionDetails);
    }

    private Transaction createTransaction(TransactionType transactionType, User transactionUser) {
        Transaction newTransaction = new Transaction();
        newTransaction.setType(transactionType);
        newTransaction.setAccount(transactionUser.getAccount());
        // local time
        newTransaction.setRegistrationTime(new Date());
        return newTransaction;
    }
}
