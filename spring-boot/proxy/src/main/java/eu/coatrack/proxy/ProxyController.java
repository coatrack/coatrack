package eu.coatrack.proxy;

/*-
 * #%L
 * ygg-proxy
 * %%
 * Copyright (C) 2013 - 2019 Corizon
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

import org.springframework.boot.SpringBootVersion;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.SpringVersion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author perezdf
 */
// curl -X POST http://<host>:<port>/refresh
@RefreshScope
@Controller
public class ProxyController extends org.springframework.cloud.netflix.zuul.web.ZuulController {
    
    @RequestMapping("/proxy")
    public void home(Model model) {
        System.out.print("test");
    }
}
