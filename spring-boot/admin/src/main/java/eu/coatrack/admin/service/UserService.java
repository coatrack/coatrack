package eu.coatrack.admin.service;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.admin.validator.UserValidator;
import eu.coatrack.admin.model.repository.TransactionRepository;
import eu.coatrack.api.CreditAccount;
import eu.coatrack.api.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Slf4j
@Service
public class UserService {

    @Value("${ygg.mail.verify-new-users-via-mail}")
    private boolean verifyNewUsersViaEmail;

    @Value("${ygg.mail.sender.user}")
    private String mail_sender_user;

    @Value("${ygg.mail.sender.password}")
    private String mail_sender_password;

    @Value("${ygg.mail.server.url}")
    private String mail_server_url;

    @Value("${ygg.mail.server.port}")
    private int mail_server_port;

    @Value("${ygg.mail.verification.server.url}")
    private String mail_verification_server_url;

    @Value("${ygg.mail.sender.from}")
    private String mail_sender_from;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserValidator userValidator;

    @Autowired
    private TransactionRepository transactionRepository;

    @ModelAttribute("user")
    public User getUserObject() {
        return new User();
    }

    public String registerForm(User user, Model model) {
        if (user == null)
            user = getUserObject();

        model.addAttribute("user", user);
        return "register";
    }

    public String registerUser(User user, BindingResult bindingResult, Model model) throws MessagingException {
        userValidator.validate(user, bindingResult);
        String result = "redirect:/admin";

        if (!bindingResult.hasErrors()) {
            user.setInitialized(Boolean.FALSE);
            CreditAccount creditAccount = new CreditAccount();
            user.setAccount(creditAccount);
            creditAccount.setUser(user);

            userRepository.save(user);

            if (verifyNewUsersViaEmail) sendVerificationEmail(user);

        } else
            result = "register";
        return result;
    }

    public ModelAndView userEmailVeritification(Long id, String emailVerificationCode) {
        User user = userRepository.findById(id).orElse(null);
        ModelAndView mav = new ModelAndView();

        if(user != null && user.getEmailVerifiedUrl().equals(emailVerificationCode) ) {
            user.setEmailVerified(true);
            userRepository.save(user);
            mav.setViewName("verified");
        } else {
            mav.setViewName("register");
            // TODO handle nullptr -> mav = ?
        }
        return mav;
    }

    public User me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName());
    }
    private void sendVerificationEmail(User user) throws MessagingException {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mail_server_url);
        mailSender.setPort(mail_server_port);
        mailSender.setProtocol("smtp");

        mailSender.setUsername(mail_sender_user);
        mailSender.setPassword(mail_sender_password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");
        props.put("mail.smtp.ssl.enable", "true");

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(user.getEmail());
        helper.setFrom(mail_sender_from);
        helper.setSubject("Verification of your email address");
        helper.setText("Dear Sir or Madam </p></p></p> In order to verify your email address, please open the following link: </p> <p><a \n"
                + "href=\"" + mail_verification_server_url + "/users/" + user.getId() + "/verify/" + user.getEmailVerifiedUrl() + "\">Click</a></p>\n"
                + "\n"
                + "<p>Best regards</p>\n"
                + "\n"
                + "<p>Coatrack Team</p>", true);
        mailSender.send(message);
    }



}
