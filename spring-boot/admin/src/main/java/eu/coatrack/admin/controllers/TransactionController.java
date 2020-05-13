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

    private SimpleDateFormat df = new SimpleDateFormat("dd/MM/YYYY");

    @Autowired
    private TransactionRepository transactionRepository;

    @RequestMapping(value = "/findByTypeWithdrawal", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public DataTableView findByType() {
        List<Transaction> result = transactionRepository.findByType(TransactionType.WITHDRAWAL);

        List<List<String>> dataTable = new ArrayList();

        result.stream().sorted((o1, o2) -> {
            return o1.getRegistrationTime().compareTo(o2.getRegistrationTime());
        }).map((item) -> {
            List<String> dataTableItem = new ArrayList<>();
            dataTableItem.add(df.format(item.getRegistrationTime()));
            dataTableItem.add(item.getDescription());
            dataTableItem.add(Double.toString(item.getAmount()));
            return dataTableItem;
        }).forEachOrdered((dataTableItem) -> {
            dataTable.add(dataTableItem);
        });

        DataTableView data = new DataTableView();
        data.setData(dataTable);

        return data;
    }

    @RequestMapping(value = "/findByTypeAnyDeposit", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public DataTableView findByTypeAnyDeposit() {

        List<Transaction> result = new ArrayList<>();
        Stream.of(transactionRepository.findByType(TransactionType.DEPOSIT)).forEach(result::addAll);
        Stream.of(transactionRepository.findByType(TransactionType.SERVICE_DEPOSIT)).forEach(result::addAll);

        Collections.sort(result, (o1, o2) -> {

            return o1.getRegistrationTime().compareTo(o2.getRegistrationTime());
        });

        List<List<String>> dataTable = new ArrayList();

        result.stream().sorted((o1, o2) -> {
            return o1.getRegistrationTime().compareTo(o2.getRegistrationTime());
        }).map((item) -> {
            List<String> dataTableItem = new ArrayList<>();
            dataTableItem.add(df.format(item.getRegistrationTime()));
            dataTableItem.add(item.getType().getDisplayString());
            dataTableItem.add(item.getDescription());
            dataTableItem.add(Double.toString(item.getAmount()));
            return dataTableItem;
        }).forEachOrdered((dataTableItem) -> {
            dataTable.add(dataTableItem);
        });
        DataTableView data = new DataTableView();
        data.setData(dataTable);

        return data;
    }

}
