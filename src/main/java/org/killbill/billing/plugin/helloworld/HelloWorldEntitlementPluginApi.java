/*
 * Copyright 2020-2024 Equinix, Inc
 * Copyright 2014-2024 The Billing Project, LLC
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

import java.util.List;

import org.killbill.billing.catalog.api.Plan;
import org.killbill.billing.entitlement.api.BaseEntitlementWithAddOnsSpecifier;
import org.killbill.billing.entitlement.api.Subscription;
import org.killbill.billing.entitlement.api.SubscriptionApiException;
import org.killbill.billing.entitlement.api.SubscriptionBundle;
import org.killbill.billing.entitlement.plugin.api.EntitlementContext;
import org.killbill.billing.entitlement.plugin.api.EntitlementPluginApi;
import org.killbill.billing.entitlement.plugin.api.EntitlementPluginApiException;
import org.killbill.billing.entitlement.plugin.api.OnFailureEntitlementResult;
import org.killbill.billing.entitlement.plugin.api.OnSuccessEntitlementResult;
import org.killbill.billing.entitlement.plugin.api.OperationType;
import org.killbill.billing.entitlement.plugin.api.PriorEntitlementResult;
import org.killbill.billing.entitlement.plugin.api.boilerplate.plugin.PriorEntitlementResultImp;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.PluginProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorldEntitlementPluginApi implements EntitlementPluginApi {

    public static final Logger logger = LoggerFactory.getLogger(HelloWorldEntitlementPluginApi.class);

    private final OSGIKillbillAPI killbillAPI;

    public HelloWorldEntitlementPluginApi(final OSGIKillbillAPI killbillAPI) {
        this.killbillAPI = killbillAPI;
    }

    @Override
    public PriorEntitlementResult priorCall(final EntitlementContext context, final Iterable<PluginProperty> properties) throws EntitlementPluginApiException {
        if (context.getOperationType() == OperationType.CREATE_SHOPPING_CART_SUBSCRIPTIONS || context.getOperationType() == OperationType.CREATE_SUBSCRIPTION || context.getOperationType() == OperationType.CREATE_SUBSCRIPTIONS_WITH_AO) {
            Iterable<BaseEntitlementWithAddOnsSpecifier> baseEntList = context.getBaseEntitlementWithAddOnsSpecifiers();
            String newplanName = baseEntList.iterator().next().getEntitlementSpecifier().iterator().next().getPlanPhaseSpecifier().getPlanName();
            logger.info("New planName: {}", newplanName);
            List<SubscriptionBundle> bundles = null;
            try {
                bundles = killbillAPI.getSubscriptionApi().getSubscriptionBundlesForAccountId(context.getAccountId(), context);
                for (SubscriptionBundle bundle : bundles) {
                    List<Subscription> subscriptions = bundle.getSubscriptions();
                    for (Subscription subscription : subscriptions) {
                        Plan plan = subscription.getLastActivePlan();
                        logger.info("Plan is {}", plan.getName());
                        if (plan.getName().equals(newplanName)) {
                            logger.info("Subscription already exists, blocking operation");
                            return new PriorEntitlementResultImp.Builder<>().withIsAborted(true).build();
                        }
                    }
                }
            } catch (SubscriptionApiException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Override
    public OnSuccessEntitlementResult onSuccessCall(final EntitlementContext context, final Iterable<PluginProperty> properties) throws EntitlementPluginApiException {
        return null;
    }

    @Override
    public OnFailureEntitlementResult onFailureCall(final EntitlementContext context, final Iterable<PluginProperty> properties) throws EntitlementPluginApiException {
        return null;
    }
}
