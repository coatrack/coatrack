package eu.coatrack.admin.controllers;

/*-
 * #%L
 * coatrack-admin
 * %%
 * Copyright (C) 2013 - 2020 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import eu.coatrack.admin.model.repository.*;
import eu.coatrack.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(path = "/api/metricsTransmission")
public class MetricsController {

    Logger log = LoggerFactory.getLogger(MetricsController.class);

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

    // TODO test with Autowired
    AntPathMatcher parser = new AntPathMatcher();

    @RequestMapping(method = RequestMethod.POST)
    public Long storeMetricAndProperlySetRelationships(
            @RequestParam("proxyId") String proxyId,
            @RequestParam("apiKeyValue") String apiKeyValue,
            @RequestBody Metric metricSubmitted) {

        log.debug("metric {} received from proxy {} with api key {}", metricSubmitted, proxyId, apiKeyValue);

        // the following related entities are NOT included in metricSubmitted, therefore IDs sent separately
        Proxy proxy = proxyRepository.findById(proxyId).orElse(null);
        ApiKey apiKey = apiKeyRepository.findByKeyValue(apiKeyValue);

        Metric metricToBeStored = null;

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

        log.debug("store metric {} properly for proxy {} and api key {}", metricToBeStored, proxyId, apiKeyValue);
        metricToBeStored.setProxy(proxy);
        metricToBeStored.setApiKey(apiKey);

        metricToBeStored = metricRepository.save(metricToBeStored);

        // The proxy calls CoatRack admin two times:
        //  1 - once for the request to the gateway
        //  2 - once for the response from the provider service
        // Only case (2) is relevant for the credit transfer.
        // In addition, the response code should be checked for number "< 400", because 4xx codes refer to client side error while the 5xx codes are related to server errors
        // So only http response codes indicating successful calls should lead to a credit transfer
        if(metricSubmitted.getHttpResponseCode() != null && metricSubmitted.getHttpResponseCode() < 400) {
            log.debug("credit transfer from consumer to provider");
            // credit transfer from consumer to provider
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
                                if (entryPoint.getHttpMethod().equals(metricSubmitted.getRequestMethod()) && !entryPoint.getHttpMethod().equals("*")
                                        || entryPoint.getHttpMethod().equals("*")) {
                                    price = price.add(new BigDecimal(entryPoint.getPricePerCall() / 1000));

                                    addTransactionDetails(consumerTransaction, price);
                                    addTransactionDetails(providerTransaction, price);

                                    log.debug("Transferring {} EUR from {} to {}",price.doubleValue(),consumer.getUsername(),provider.getUsername());

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

        return metricToBeStored.getId();
    }

    // This method adds additional information and stores all the transaction details
    private void addTransactionDetails(Transaction transactionDetails, BigDecimal price) {

        transactionDetails.setAmount(price.doubleValue());

        transactionRepository.save(transactionDetails);

        transactionDetails.getAccount().getTransaction().add(transactionDetails);
    }

    // This method creates a new Transaction
    private Transaction createTransaction(TransactionType transactionType, User transactionUser) {

        Transaction newTransaction = new Transaction();
        newTransaction.setType(transactionType);
        newTransaction.setAccount(transactionUser.getAccount());

        // local time
        newTransaction.setRegistrationTime(new Date());

        return newTransaction;
    }

}
