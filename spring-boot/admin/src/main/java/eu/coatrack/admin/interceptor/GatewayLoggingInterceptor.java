package eu.coatrack.admin.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

@Component
public class GatewayLoggingInterceptor extends HandlerInterceptorAdapter {

    private static final Logger log = LoggerFactory.getLogger(GatewayLoggingInterceptor.class);

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        log.info(String.format("The url called is: '%s'", request.getRequestURI()));

        Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.info(String.format("Header Name - " + headerName + ", Value - " + request.getHeader(headerName)));
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        log.info(String.format("The url called is: '%s'", request.getRequestURI()));
        Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.info(String.format("Header Name - " + headerName + ", Value - " + request.getHeader(headerName)));
        }
    }
}
