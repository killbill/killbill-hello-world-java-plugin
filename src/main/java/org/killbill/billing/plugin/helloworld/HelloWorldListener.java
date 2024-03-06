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

import org.joda.time.LocalDate;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
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
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorldListener implements OSGIKillbillEventDispatcher.OSGIKillbillEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldListener.class);

    private final OSGIKillbillAPI osgiKillbillAPI;

    private final ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> invoiceFormatterTracker;

    public HelloWorldListener(final OSGIKillbillAPI killbillAPI, final ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> invoiceFormatterTracker) {
        this.osgiKillbillAPI = killbillAPI;
        this.invoiceFormatterTracker = invoiceFormatterTracker;
    }

    private static final String defaultLocale = "en_US";

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {
        logger.info("Received event {} for object id {} of type {}",
                    killbillEvent.getEventType(),
                    killbillEvent.getObjectId(),
                    killbillEvent.getObjectType());

        final TenantContext context = new PluginTenantContext(killbillEvent.getAccountId(), killbillEvent.getTenantId());
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
                break;
            case INVOICE_CREATION:

                final Account account;
                try {
                    account = osgiKillbillAPI.getAccountUserApi().getAccountById(killbillEvent.getAccountId(), context);
                } catch (AccountApiException e) {
                    throw new RuntimeException(e);
                }
                final List<Invoice> invoices = osgiKillbillAPI.getInvoiceUserApi().getInvoicesByAccount(killbillEvent.getAccountId(), false, false, true, context);
                logger.info("Invoices in hello-world-plugin {}: ",invoices.size());
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
