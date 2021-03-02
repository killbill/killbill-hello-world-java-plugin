/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.helloworld;

import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jooby.mvc.GET;
import org.jooby.mvc.Local;
import org.jooby.mvc.Path;
import org.killbill.billing.tenant.api.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
@Path("/")
public class HelloWorldServlet {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldServlet.class);

    public HelloWorldServlet() {
    }

    /**
     * Kill Bill automatically injects Tenant object in this method when this end point is accessed with the X-Killbill-ApiKey and X-Killbill-ApiSecret headers 
     * @param tenant
     */
    @GET
    public void hello(@Local @Named("killbill_tenant") final Optional<Tenant> tenant) {
        // Find me on http://127.0.0.1:8080/plugins/hello-world-plugin
        logger.info("Hello world");
        if(tenant != null && tenant.isPresent() ) {
        	logger.info("tenant is available");
        	Tenant t1 = tenant.get();
        	logger.info("tenant id:"+t1.getId());
        }
        else {
        	logger.info("tenant is not available");
        }
    }
}
