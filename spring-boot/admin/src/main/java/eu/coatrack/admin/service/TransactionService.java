package eu.coatrack.admin.service;

import eu.coatrack.admin.model.repository.TransactionRepository;
import eu.coatrack.api.DataTableView;
import eu.coatrack.api.Transaction;
import eu.coatrack.api.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
public class TransactionService {

    private final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");

    @Autowired
    private TransactionRepository transactionRepository;

    public DataTableView findByType() {
        List<Transaction> result = transactionRepository.findByType(TransactionType.WITHDRAWAL);

        List<List<String>> dataTable = new ArrayList<>();

        result.stream().sorted(Comparator.comparing(Transaction::getRegistrationTime)).map((item) -> {
            List<String> dataTableItem = new ArrayList<>();
            dataTableItem.add(df.format(item.getRegistrationTime()));
            dataTableItem.add(item.getDescription());
            dataTableItem.add(Double.toString(item.getAmount()));
            return dataTableItem;
        }).forEachOrdered(dataTable::add);

        DataTableView data = new DataTableView<>();
        data.setData(dataTable);

        return data;
    }

    public DataTableView findByTypeAnyDeposit() {

        List<Transaction> result = new ArrayList<>();
        Stream.of(transactionRepository.findByType(TransactionType.DEPOSIT)).forEach(result::addAll);
        Stream.of(transactionRepository.findByType(TransactionType.SERVICE_DEPOSIT)).forEach(result::addAll);

        result.sort(Comparator.comparing(Transaction::getRegistrationTime));

        List<List<String>> dataTable = new ArrayList<>();

        result.stream().sorted(Comparator.comparing(Transaction::getRegistrationTime)).map((item) -> {
            List<String> dataTableItem = new ArrayList<>();
            dataTableItem.add(df.format(item.getRegistrationTime()));
            dataTableItem.add(item.getType().getDisplayString());
            dataTableItem.add(item.getDescription());
            dataTableItem.add(Double.toString(item.getAmount()));
            return dataTableItem;
        }).forEachOrdered(dataTable::add);

        DataTableView data = new DataTableView();
        data.setData(dataTable);

        return data;
    }
}
