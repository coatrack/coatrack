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
import com.paypal.base.rest.PayPalRESTException;
import eu.coatrack.admin.service.PaypalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@CrossOrigin(origins = "*")
@Controller
public class PaypalController {

    @Autowired
    private PaypalService payPalService;

    @RequestMapping("/catchCancelResponse")
    public String catchCancelResponse() {
        return payPalService.catchCancelResponse();
    }

    @RequestMapping("/catchPaymentResponse")
    public String catchPaymentResponse(Authentication authentication, @RequestParam(name = "paymentId") String paymentId, @RequestParam(name = "PayerID") String payerID) throws PayPalRESTException {
        return payPalService.catchPaymentResponse(authentication, paymentId, payerID);
    }

    @RequestMapping("/pay5euros")
    public String pay5euros(Authentication authentication) throws PayPalRESTException {
        return payPalService.pay5euros(authentication);
    }

}
