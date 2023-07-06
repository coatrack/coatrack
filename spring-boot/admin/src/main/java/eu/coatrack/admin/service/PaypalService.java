package eu.coatrack.admin.service;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import eu.coatrack.admin.model.repository.TransactionRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.api.Transaction;
import eu.coatrack.api.TransactionType;
import eu.coatrack.api.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class PaypalService {

    private static final String yggID = "AT46LAhyvMCNIPmEqmX3vHljZl-0hdn3viQteO_3Sj3Zjy3NwHJ4gY_y6mIFwQ2_bApuz8qh32V-lXes";
    private static final String yggSecret = "ELaUOv4-ljLLXhy44NMzH5omOG19YePUxdoiyPLjUEqAk2MWQF1P_3fS9R5tyoFE7p713uulBwe-ye4f";

    private static final String executionMode = "sandbox"; // sandbox or production

    // Pass the clientID, secret and mode. The easiest, and most widely used option.
    private static final APIContext yggapiContext = new APIContext(yggID, yggSecret, executionMode);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public String catchCancelResponse() {
        return "home";
    }

    public String catchPaymentResponse(Authentication authentication, String paymentId, String payerID) throws PayPalRESTException {
        Payment payment = new Payment();
        payment.setId(paymentId);

        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(payerID);
        Payment createdPayment = payment.execute(yggapiContext, paymentExecution);

        List<Transaction> transactions = transactionRepository.findByPaymentId(paymentId);
        transactions.get(0).setType(TransactionType.DEPOSIT);
        transactionRepository.save(transactions.get(0));

        User user = userRepository.findByUsername(authentication.getName());

        double actual = user.getAccount().getBalance();
        user.getAccount().setBalance(actual + 5);

        return "redirect:/admin/profiles";
    }

    public String pay5euros(Authentication authentication) throws PayPalRESTException {

        /*
         * Flow would look like this:
         * 1. Create Payer object and set PaymentMethod
         * 2. Set RedirectUrls and set cancelURL and returnURL
         * 3. Set Details and Add PaymentDetails
         * 4. Set Amount
         * 5. Set Transaction
         * 6. Add Payment Details and set Intent to "authorize"
         * 7. Create APIContext by passing the clientID, secret and mode
         * 8. Create Payment object and get paymentID
         * 9. Set payerID to PaymentExecution object
         * 10. Execute Payment and get Authorization
         *
         */
        Payer yggPayer = new Payer();
        yggPayer.setPaymentMethod("paypal");

        // Redirect URLs
        RedirectUrls yggRedirectUrls = new RedirectUrls();
        yggRedirectUrls.setCancelUrl("http://localhost:8080/catchCancelResponse");
        yggRedirectUrls.setReturnUrl("http://localhost:8080/catchPaymentResponse");

        // Set Payment Details Object
        Details yggDetails = new Details();
        yggDetails.setShipping("0.00");
        yggDetails.setSubtotal("5.00");
        yggDetails.setTax("0.00");

        // Set Payment amount
        Amount yggAmount = new Amount();
        yggAmount.setCurrency("EUR");
        yggAmount.setTotal("5.00");
        yggAmount.setDetails(yggDetails);

        // Set Transaction information
        com.paypal.api.payments.Transaction yggTransaction = new com.paypal.api.payments.Transaction();
        yggTransaction.setAmount(yggAmount);
        yggTransaction.setDescription("5 Euros Coatrack Deposit");
        List<com.paypal.api.payments.Transaction> yggTransactions = new ArrayList<>();
        yggTransactions.add(yggTransaction);

        // Add Payment details
        Payment yggPayment = new Payment();

        // Set Payment intent to authorize
        yggPayment.setIntent("authorize");
        yggPayment.setPayer(yggPayer);
        yggPayment.setTransactions(yggTransactions);
        yggPayment.setRedirectUrls(yggRedirectUrls);

        Payment myPayment = yggPayment.create(yggapiContext);

        User user = userRepository.findByUsername(authentication.getName());

        eu.coatrack.api.Transaction transaction = new eu.coatrack.api.Transaction();
        transaction.setAmount(5.0);
        transaction.setDescription("Payment by paypal");
        transaction.setPaymentId(myPayment.getId());
        transaction.setType(TransactionType.DEPOSIT_IN_TRANSIT);
        transaction.setOwner(user);
        transaction.setRegistrationTime(new Date());
        transaction.setAccount(user.getAccount());
        user.getAccount().getTransaction().add(transaction);
        userRepository.save(user);
        transactionRepository.save(transaction);
        return "redirect:" + myPayment.getLinks().get(1).getHref();

    }
}
