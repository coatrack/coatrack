package eu.coatrack.admin.service;

import eu.coatrack.admin.model.repository.ApiKeyRepository;
import eu.coatrack.admin.model.repository.ProxyRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.api.ApiKey;
import eu.coatrack.api.Proxy;
import eu.coatrack.api.ServiceApi;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GatewayApiService {
    @Autowired
    ApiKeyRepository apiKeyRepository;

    @Autowired
    ServiceApiRepository serviceApiRepository;

    @Autowired
    ProxyRepository proxyRepository;

    public ApiKey findApiKeyEntityByApiKeyValue(String apiKeyValue) {
        return apiKeyRepository.findByKeyValue(apiKeyValue);
    }

    public ServiceApi findServiceByApiKeyValue(String apiKeyValue) {
        return serviceApiRepository.findByApiKeyValue(apiKeyValue);
    }

    public ResponseEntity<List<ApiKey>> findApiKeyListByGatewayApiKey(String gatewayIdAndApiKey) {
        log.debug("The gateway with the ID {} requests its latest API key list.", gatewayIdAndApiKey);
        ResponseEntity<List<ApiKey>> result;
        try {
            Optional<Proxy> callingProxy = proxyRepository.findById(gatewayIdAndApiKey);
            if (callingProxy.isPresent()) {
                Proxy proxyToUpdate = callingProxy.get();
                proxyToUpdate.updateTimeOfLastSuccessfulCallToAdmin_setToNow();
                proxyRepository.save(proxyToUpdate);
                result = new ResponseEntity<>(getApiKeysBelongingToServicesOf(proxyToUpdate), HttpStatus.OK);
            } else {
                result = new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.warn("The creation of the API key list for the gateway {} failed.", gatewayIdAndApiKey, e);
            result = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return result;
    }

    private List<ApiKey> getApiKeysBelongingToServicesOf(Proxy proxy) {
        List<ApiKey> result;
        if (proxy.getServiceApis() == null || proxy.getServiceApis().isEmpty()) {
            log.debug("The gateway with the ID {} does not provide any services.", proxy.getId());
            result = new ArrayList<>();
        } else {
            result = proxy.getServiceApis().stream()
                    .flatMap(serviceApi -> serviceApi.getApiKeys().stream())
                    .collect(Collectors.toList());
        }
        return result;
    }
}
