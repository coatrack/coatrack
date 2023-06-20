package eu.coatrack.admin.service;

import eu.coatrack.admin.model.repository.BankAccountRepository;
import eu.coatrack.admin.model.repository.CreditAccountRepository;
import eu.coatrack.admin.model.repository.TransactionRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
public class CreditAccountService {

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

    public CreditAccount getAccount(Long id) {
        return creditAccountRepository.findById(id).orElse(null);
    }

    public CreditAccount withdrawal(Authentication authentication, Transaction amount) throws MessagingException {

        // Retrieve bank account
        StringBuilder sendAmountTo = new StringBuilder();
        User user = userRepository.findByUsername(authentication.getName());
        List<BankAccount> accounts = user.getAccount().getBankAccount();
        for (BankAccount account : accounts) {
            if (account.isDefaultAccount())
                sendAmountTo.append(account);
        }

        // Calculate amount after commision
        double amountResult = amount.getAmount() - commisionFix - (amount.getAmount() * commisionVariable / 100);
        double balanceResult = amount.getAmount() + commisionFix + (amount.getAmount() * commisionVariable / 100);

        user.getAccount().setBalance(user.getAccount().getBalance() - balanceResult);
        creditAccountRepository.save(user.getAccount());

        sendBillToBookKeeping(amountResult, sendAmountTo.toString());

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

    private void sendBillToBookKeeping(double amountResult, String sendAmountTo) throws MessagingException {
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
        helper.setText("Coatrack server requests you to: </p></p>"
                + "</p> make a bank transfer of " + amountResult + " to: </p> <p>"
                + sendAmountTo
                + "\n"
                + "<p>Best regards</p>\n"
                + "\n"
                + "<p>Coatrack Team</p>", true);
        mailSender.send(message);
    }

    public CreditAccount postBankAccount(Authentication authentication, BankAccount bankAccount) {
        User user = userRepository.findByUsername(authentication.getName());

        if(user.getAccount().getBankAccount().isEmpty())
            bankAccount.setDefaultAccount(true);

        user.getAccount().getBankAccount().add(bankAccount);
        bankAccount.setAccount(user.getAccount());
        bankAccountRepository.save(bankAccount);
        CreditAccount accountSaved = creditAccountRepository.save(user.getAccount());
        userRepository.save(user);

        return accountSaved;
    }

    public CreditAccount setDefaultBankAccount(Authentication authentication, long id) {
        User user = userRepository.findByUsername(authentication.getName());
        List<BankAccount> bankAccounts = user.getAccount().getBankAccount();

        bankAccounts.stream().map((bankAccount) -> {
            bankAccount.setDefaultAccount(false);
            return bankAccount;
        }).forEachOrdered((bankAccount) -> bankAccountRepository.save(bankAccount));

        BankAccount bankAccount = bankAccountRepository.findById(id).orElse(null);
        if(bankAccount != null) {
            bankAccount.setDefaultAccount(true);
            bankAccountRepository.save(bankAccount);
        }
        return user.getAccount();
    }

}
