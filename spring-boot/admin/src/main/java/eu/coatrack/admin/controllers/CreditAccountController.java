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

import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import eu.coatrack.admin.model.repository.BankAccountRepository;
import eu.coatrack.admin.model.repository.CreditAccountRepository;
import eu.coatrack.admin.model.repository.TransactionRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.admin.service.CreditAccountService;
import eu.coatrack.api.BankAccount;
import eu.coatrack.api.CreditAccount;
import eu.coatrack.api.Transaction;
import eu.coatrack.api.TransactionType;
import eu.coatrack.api.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/admin/creditAccount")
public class CreditAccountController {

    @Autowired
    private CreditAccountService creditAccountService;

    @RequestMapping(value = "/{id}/", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public CreditAccount getAccount(@PathVariable("id") Long id) {
        return creditAccountService.getAccount(id);
    }

    @RequestMapping(value = "/withDrawal", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public CreditAccount withdrawal(Authentication authentication, @RequestBody Transaction amount) throws MessagingException {
        return creditAccountService.withdrawal(authentication, amount);
    }

    @RequestMapping(value = "/bankAccount", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public CreditAccount postBankAccount(Authentication authentication, @RequestBody BankAccount bankAccount) {
        return creditAccountService.postBankAccount(authentication, bankAccount);
    }

    @RequestMapping(value = "/bankAccount/{id}/setDefault")
    @ResponseBody
    public CreditAccount setDefaultBankAccount(Authentication authentication, @PathVariable("id") long id) {
        return creditAccountService.setDefaultBankAccount(authentication, id);
    }

}
