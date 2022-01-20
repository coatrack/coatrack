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
import javax.mail.MessagingException;
import eu.coatrack.admin.model.repository.TransactionRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.admin.service.mail.MailService;
import eu.coatrack.admin.validator.UserValidator;
import eu.coatrack.api.CreditAccount;
import eu.coatrack.api.User;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author gr-hovest(at)atb-bremen.de
 */
@Controller
@RequestMapping(value = "/")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserValidator userValidator;

    @ModelAttribute("user")
    public User getUserObject() {
        return new User();
    }

    @RequestMapping("/register")
    public String registerForm(User user, BindingResult bindingResult, Model model) {

        if (user == null) {
            user = getUserObject();
        }

        model.addAttribute("user", user);

        return "register";
    }

    @Value("${ygg.mail.verification.server.url}")
    private String mail_verification_server_url;

    @Value("${ygg.mail.sender.from}")
    private String mail_sender_from;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MailService mailservice;

    @PostMapping(value = "/register")
    public String registerUser(User user, BindingResult bindingResult, Model model) throws MessagingException, IOException {

        userValidator.validate(user, bindingResult);

        if (bindingResult.hasErrors()) {

            return "register";
        }

        user.setInitialized(Boolean.FALSE);

        CreditAccount creditAccount = new CreditAccount();

        user.setAccount(creditAccount);

        userRepository.save(user);

        String user_to = user.getEmail();
        String user_from = mail_sender_from;
        String subject = "Verification of your email address";
        String body = "Dear Sir or Madam </p></p></p> In order to verify your email address, please open the following link: </p> <p><a \n"
                + "href=\"" + mail_verification_server_url + "/users/" + user.getId() + "/verify/" + user.getEmailVerifiedUrl() + "\">Click</a></p>\n"
                + "\n"
                + "<p>Best regards</p>\n"
                + "\n"
                + "<p>Coatrack Team</p>";
        mailservice.send(user_from, user_to, subject, body, true);

        return "redirect:/admin";
    }

    @GetMapping(value = "users/{id}/verify/{emailVerificationCode}")
    public ModelAndView userEmailVeritification(@PathVariable("id") Long id, @PathVariable("emailVerificationCode") String emailVerificationCode) {

        User user = userRepository.findOne(id);

        if (user.getEmailVerifiedUrl().equals(emailVerificationCode)) {
            user.setEmailVerified(Boolean.TRUE);
        }
        userRepository.save(user);

        ModelAndView mav = new ModelAndView();

        mav.setViewName("verified");
        return mav;
    }

    @RequestMapping(value = "/me", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public User me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        return user;
    }
}
