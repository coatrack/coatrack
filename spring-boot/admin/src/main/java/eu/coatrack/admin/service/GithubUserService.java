package eu.coatrack.admin.service;

import eu.coatrack.api.DataTableView;
import eu.coatrack.config.github.GithubUserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Service
public class GithubUserService {
    @Autowired
    private GithubService githubService;

    public DataTableView<GithubUserProfile> getEmptyDataTableView() {
        DataTableView<GithubUserProfile> table = new DataTableView<>();
        table.setData(new ArrayList<>());
        return table;
    }

    public DataTableView<GithubUserProfile> getDataTableViewByCriteria(String username) throws IOException {
        DataTableView<GithubUserProfile> table = new DataTableView<>();
        table.setData(githubService.findGithubUserProfileByCriteria(username));
        return table;
    }
}
