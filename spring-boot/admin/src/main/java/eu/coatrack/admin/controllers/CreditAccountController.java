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
    private CreditAccountRepository creditAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Value("${ygg.admin.payment.commision.variable}")
    private Long commisionVariable;

    @Value("${ygg.admin.payment.commision.fix}")
    private Long commisionFix;

    @Value("${ygg.admin.payment.commision.bookKeeping.contact}")
    private String bookKeepingContact;

    @Value("${ygg.mail.sender.user}")
    private String mail_sender_user;

    @Value("${ygg.mail.sender.password}")
    private String mail_sender_password;

    @Autowired
    private TransactionRepository transactionRepository;

    @RequestMapping(value = "/{id}/", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public CreditAccount getAccount(
            @PathVariable("id") Long id
    ) {
        return creditAccountRepository.findById(id).orElse(null);
    }

    @RequestMapping(value = "/withDrawal", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public CreditAccount withdrawal(
            Authentication authentication, @RequestBody Transaction amount
    ) throws MessagingException {

        // Retrieve bank account
        String sendAmountTo = "";
        User user = userRepository.findByUsername(authentication.getName());
        List<BankAccount> accounts = user.getAccount().getBankAccount();
        for (BankAccount account : accounts) {
            if (account.isDefaultAccount()) {
                sendAmountTo = account.getAccountHolder() + " " + account.getBankName() + " " + account.getIban();
            }
        }

        // Calculate amount after commision
        double amountResult = amount.getAmount() - commisionFix - (amount.getAmount() * commisionVariable / 100);
        double balanceResult = amount.getAmount() + commisionFix + (amount.getAmount() * commisionVariable / 100);

        user.getAccount().setBalance(user.getAccount().getBalance() - balanceResult);
        creditAccountRepository.save(user.getAccount());

        // Send communication
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setProtocol("smtp");

        mailSender.setUsername(mail_sender_user);
        mailSender.setPassword(mail_sender_password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(bookKeepingContact);
        helper.setFrom("payment@coatrack.eu");
        helper.setSubject("Withdrawal Request");
        helper.setText("Coatrack server resquest you to: </p></p>"
                + "</p> make a bank transfer of " + Double.toString(amountResult) + " to: </p> <p>"
                + sendAmountTo
                + "\n"
                + "<p>Best regards</p>\n"
                + "\n"
                + "<p>Coatrack Team</p>", true);
        mailSender.send(message);

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setAmount(amount.getAmount());
        transaction.setDescription("Withdrawal request to :" + sendAmountTo);
        transaction.setOwner(user);
        transaction.setRegistrationTime(new Date());
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setAccount(user.getAccount());
        transactionRepository.save(transaction);

        user.getAccount().getTransaction().add(transaction);

        return user.getAccount();
    }

    @RequestMapping(value = "/bankAccount", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public CreditAccount postBankAccount(
            Authentication authentication, @RequestBody BankAccount bankAccount
    ) {

        User user = userRepository.findByUsername(authentication.getName());
        if(user.getAccount().getBankAccount().isEmpty())
        {
            bankAccount.setDefaultAccount(true);
        }
        user.getAccount().getBankAccount().add(bankAccount);
        bankAccount.setAccount(user.getAccount());
        bankAccountRepository.save(bankAccount);
        CreditAccount accountSaved = creditAccountRepository.save(user.getAccount());
        userRepository.save(user);

        return accountSaved;
    }

    @RequestMapping(value = "/bankAccount/{id}/setDefault")
    @ResponseBody
    public CreditAccount setDefaultBankAccount(
            Authentication authentication, @PathVariable("id") long id
    ) {
        User user = userRepository.findByUsername(authentication.getName());
        List<BankAccount> bankAccounts = user.getAccount().getBankAccount();

        bankAccounts.stream().map((bankAccount) -> {
            bankAccount.setDefaultAccount(false);
            return bankAccount;
        }).forEachOrdered((bankAccount) -> {
            bankAccountRepository.save(bankAccount);
        });

        BankAccount bankAccount = bankAccountRepository.findById(id).orElse(null);
        bankAccount.setDefaultAccount(true);
        bankAccountRepository.save(bankAccount);

        return user.getAccount();
    }

}
