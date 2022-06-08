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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import eu.coatrack.admin.model.repository.TransactionRepository;
import eu.coatrack.admin.service.TransactionService;
import eu.coatrack.api.DataTableView;
import eu.coatrack.api.Transaction;
import eu.coatrack.api.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author gr-hovest(at)atb-bremen.de
 */
@Controller
@RequestMapping(value = "/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @RequestMapping(value = "/findByTypeWithdrawal", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public DataTableView findByType() {
        return transactionService.findByType();
    }

    @RequestMapping(value = "/findByTypeAnyDeposit", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public DataTableView findByTypeAnyDeposit() {
        return transactionService.findByTypeAnyDeposit();
    }

}
