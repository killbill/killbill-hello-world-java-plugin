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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.entitlement.api.Subscription;
import org.killbill.billing.entitlement.api.SubscriptionApiException;
import org.killbill.billing.entitlement.api.SubscriptionEvent;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.formatters.InvoiceFormatter;
import org.killbill.billing.invoice.api.formatters.InvoiceItemFormatter;
import org.killbill.billing.invoice.plugin.api.InvoiceFormatterFactory;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.plugin.api.PluginTenantContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.commons.health.api.HealthCheckRegistry;
import org.killbill.commons.health.api.Result;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorldListener implements OSGIKillbillEventDispatcher.OSGIKillbillEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldListener.class);

    private final OSGIKillbillAPI osgiKillbillAPI;

    private final ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> invoiceFormatterTracker;

    private final Properties configProperties;

    private final HealthCheckRegistry healthCheckRegistry;

    public HelloWorldListener(final OSGIKillbillAPI killbillAPI, final ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> invoiceFormatterTracker, final HealthCheckRegistry healthCheckRegistry, final Properties configProperties) {
        this.osgiKillbillAPI = killbillAPI;
        this.invoiceFormatterTracker = invoiceFormatterTracker;
        this.healthCheckRegistry = healthCheckRegistry;
        this.configProperties = configProperties;
    }

    private static final String defaultLocale = "en_US";

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {
        logger.info("Received event {} for object id {} of type {}",
                    killbillEvent.getEventType(),
                    killbillEvent.getObjectId(),
                    killbillEvent.getObjectType());

        final TenantContext context = new PluginTenantContext(killbillEvent.getAccountId(), killbillEvent.getTenantId());
        //        HealthCheck aviateHealthCheck = hea
        switch (killbillEvent.getEventType()) {
            //
            // Handle ACCOUNT_CREATION and ACCOUNT_CHANGE only for demo purpose and just print the account
            //
            case ACCOUNT_CREATION:
            case ACCOUNT_CHANGE:
                try {
                    final Account account = osgiKillbillAPI.getAccountUserApi().getAccountById(killbillEvent.getAccountId(), context);
                    logger.info("Account information: " + account);
                } catch (final AccountApiException e) {
                    logger.warn("Unable to find account", e);
                }
                final Set<String> names = healthCheckRegistry.getNames();
                logger.info("names {}", names);
                Result result = healthCheckRegistry.runHealthCheck("org.killbill.billing.server.healthchecks.KillbillHealthcheck");
                logger.info("KB healthcheck result: {}", result.isHealthy());
                result = healthCheckRegistry.runHealthCheck("org.killbill.billing.server.healthchecks.KillbillPluginsHealthcheck");
                logger.info("Plugins healthcheck result: {}", result.isHealthy());
                Map<String, Object> pluginHealthDetails = result.getDetails();
                for(Entry<String, Object> entry: pluginHealthDetails.entrySet()) {
                    String pluginKey = entry.getKey();
                    Map<Object, Object> pluginDetails = (Map)entry.getValue();
                    logger.info("Plugin {}, Details {}", entry.getKey(), entry.getValue());
                }
                break;
            case SUBSCRIPTION_CREATION:
                try {
                    Subscription subscription = osgiKillbillAPI.getSubscriptionApi().getSubscriptionForEntitlementId(killbillEvent.getObjectId(), true, context);
                    List<SubscriptionEvent> events = subscription.getSubscriptionEvents();
                    logger.info("events:"+events);
                } catch (SubscriptionApiException e) {
                    throw new RuntimeException(e);
                }
            case INVOICE_CREATION:

                final Account account;
                try {
                    account = osgiKillbillAPI.getAccountUserApi().getAccountById(killbillEvent.getAccountId(), context);
                } catch (AccountApiException e) {
                    throw new RuntimeException(e);
                }
                final List<Invoice> invoices = osgiKillbillAPI.getInvoiceUserApi().getInvoicesByAccount(killbillEvent.getAccountId(), false, false, true, context);
                logger.info("Invoices in hello-world-plugin {}: ",invoices.size());

                final String invoiceFormatterPluginName = configProperties.getProperty("org.killbill.template.invoiceFormatterFactoryPluginName");
                if(invoiceFormatterPluginName == null || invoiceFormatterPluginName.isEmpty()){
                    logger.info("Invoice formatter plugin not configured. set the org.killbill.template.invoiceFormatterFactoryPluginName property to configure it");
                    return;
                }

                final InvoiceFormatterFactory formatterFactory = (invoiceFormatterTracker != null ? invoiceFormatterTracker.getService() : null);
                Invoice invoice = invoices.get(0); //For demo purpose, we are only retrieving the formattedEndDate for the first invoice
                //TODO Using null for parameters like catalogBundlePath,bundle, defaultBundle, etc. Update this to use correct values if possible or verify that using null values has no adverse effect

                InvoiceFormatter invoiceFormatter = formatterFactory.createInvoiceFormatter(defaultLocale, null, invoice, Locale.forLanguageTag(account.getLocale()), osgiKillbillAPI.getCurrencyConversionApi(), null, null);

                List<InvoiceItem> items = invoiceFormatter.getInvoiceItems();
                logger.info("hello-world-plugin got items:{}",items.size());
                for(InvoiceItem item:items) {
                    final InvoiceItemFormatter invoiceItemFormatter = (InvoiceItemFormatter)item;
                    final String formattedEndDate = invoiceItemFormatter.getFormattedEndDate();
                    logger.info("hello-world-plugin formattedEndDate:{}",formattedEndDate);
                }
            // Nothing
            default:
                break;

        }
    }
}
