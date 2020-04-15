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

import java.io.IOException;
import java.util.ArrayList;
import eu.coatrack.admin.service.GithubService;
import eu.coatrack.api.DataTableView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin/github")
public class GithubUserController {

    private static final Logger log = LoggerFactory.getLogger(GithubUserController.class);

    @Autowired
    private GithubService githubService;

    /**
     * This method is used to return an empty list for the AJAX default value.
     * It is temporally
     *
     * @return
     * @throws IOException
     */
    @GetMapping(value = "/search_empty", produces = "application/json")
    @ResponseBody
    public DataTableView apiKeyGithubUserListPageRestEmpty() throws IOException {
        DataTableView table = new DataTableView();
        table.setData(new ArrayList());

        return table;
    }

    /**
     * Based on https://developer.github.com/v3/search/#search-users
     *
     * for
     * example:https://api.github.com/search/users?q=paco+in%3Alogin+paco+in%3Aemail
     *
     * @param criteria
     * @return
     * @throws java.io.IOException
     *
     */
    @GetMapping(value = "/search/{criteria:.+}", produces = "application/json")
    @ResponseBody
    public DataTableView apiKeyGithubUserListPageRestByCriteria(@PathVariable("criteria") String criteria) throws IOException {

        DataTableView table = new DataTableView();

        table.setData(githubService.findGithubUserProfileByCriteria(criteria));

        return table;
    }

    @GetMapping(value = "/{username}", produces = "application/json")
    @ResponseBody
    public DataTableView apiKeyGithubUserListPageRestByUsername(@PathVariable("username") String username) throws IOException {

        DataTableView table = new DataTableView();

        table.setData(githubService.findGithubUserProfileByUsername(username));

        return table;
    }

}
