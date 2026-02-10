/*
 * Copyright 2020-2022 Equinix, Inc
 * Copyright 2014-2022 The Billing Project, LLC
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.invoice.plugin.api.AdditionalItemsResult;
import org.killbill.billing.invoice.plugin.api.InvoiceContext;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.api.invoice.PluginInvoiceItem;
import org.killbill.billing.plugin.api.invoice.PluginInvoicePluginApi;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HelloWorldInvoicePluginApi extends PluginInvoicePluginApi implements OSGIKillbillEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldInvoicePluginApi.class);

    static final String LOYALTY_DISCOUNT_DESC = "Loyalty discount \u2013 25% off (every 3rd purchase)";
    static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.25");
    static final int PURCHASE_CYCLE = 3;

    /**
     * Invoice item types that represent a customer charge (one-time or otherwise).
     */
    private static final Set<InvoiceItemType> CHARGEABLE_TYPES = EnumSet.of(
            InvoiceItemType.FIXED,
            InvoiceItemType.EXTERNAL_CHARGE,
            InvoiceItemType.USAGE,
            InvoiceItemType.RECURRING
    );

    public HelloWorldInvoicePluginApi(final OSGIKillbillAPI killbillAPI, final OSGIConfigPropertiesService configProperties,
                                      final Clock clock) {
        super(killbillAPI, configProperties, clock);
    }

    /**
     * Applies a 25% loyalty discount to every 3rd one-time purchase.
     * <p>
     * During invoice generation Kill Bill calls this method before the invoice is
     * committed. The plugin counts past one-time-purchase invoices for the account
     * and, when the current invoice is the 3rd (6th, 9th, …) purchase, returns
     * negative {@code ITEM_ADJ} items equal to 25% of each charge line item.
     *
     * @param newInvoice     The invoice that is being created.
     * @param dryRun         Whether it is a dry-run preview.
     * @param properties     Any user-specified plugin properties.
     * @param invoiceContext The context in which this code is running.
     * @return Additional adjustment items representing the loyalty discount, or an
     *         empty list when the discount does not apply.
     */
    @Override
    public AdditionalItemsResult getAdditionalInvoiceItems(final Invoice newInvoice, final boolean dryRun,
                                                       final Iterable<PluginProperty> properties, final InvoiceContext invoiceContext) {

        final UUID accountId = newInvoice.getAccountId();
        final Account account = getAccount(accountId, invoiceContext);
        final List<InvoiceItem> additionalItems = new LinkedList<InvoiceItem>();

        // --- Idempotency guard: skip if discount items were already added --------
        if (hasExistingLoyaltyDiscount(newInvoice)) {
            logger.info("Loyalty discount already present on invoice {} for account {} – skipping",
                        newInvoice.getId(), accountId);
            return buildResult(additionalItems);
        }

        // --- Count past one-time-purchase invoices (excluding the current one) ---
        final Collection<Invoice> pastInvoices = getInvoicesByAccountId(account.getId(), invoiceContext);
        int pastPurchaseCount = 0;
        for (final Invoice invoice : pastInvoices) {
            if (!invoice.getId().equals(newInvoice.getId()) && isOneTimePurchaseInvoice(invoice)) {
                pastPurchaseCount++;
            }
        }

        // The current invoice is purchase number (pastPurchaseCount + 1)
        // but only if this invoice itself qualifies as a one-time purchase.
        if (!isOneTimePurchaseInvoice(newInvoice)) {
            logger.debug("Invoice {} for account {} is not a one-time purchase – no loyalty discount",
                         newInvoice.getId(), accountId);
            return buildResult(additionalItems);
        }

        final int currentPurchaseNumber = pastPurchaseCount + 1;
        logger.info("Account {}: this is one-time purchase #{}", accountId, currentPurchaseNumber);

        if (currentPurchaseNumber % PURCHASE_CYCLE != 0) {
            logger.info("Account {}: purchase #{} is not a multiple of {} – no discount",
                        accountId, currentPurchaseNumber, PURCHASE_CYCLE);
            return buildResult(additionalItems);
        }

        // --- Apply 25% discount to every chargeable item on this invoice ---------
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (final InvoiceItem item : newInvoice.getInvoiceItems()) {
            if (isChargeableItem(item) && item.getAmount() != null && item.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                final BigDecimal discountAmount = item.getAmount()
                        .multiply(DISCOUNT_RATE)
                        .setScale(2, RoundingMode.HALF_UP)
                        .negate();

                final InvoiceItem adjItem = PluginInvoiceItem.createAdjustmentItem(
                        item,
                        item.getInvoiceId(),
                        newInvoice.getInvoiceDate(),
                        newInvoice.getInvoiceDate(),
                        discountAmount,
                        LOYALTY_DISCOUNT_DESC);
                additionalItems.add(adjItem);
                totalDiscount = totalDiscount.add(discountAmount);
            }
        }

        if (dryRun) {
            logger.info("Account {}: dry-run – loyalty discount of {} would be applied on purchase #{}",
                        accountId, totalDiscount, currentPurchaseNumber);
        } else {
            logger.info("Account {}: applying loyalty discount of {} on purchase #{}",
                        accountId, totalDiscount, currentPurchaseNumber);
        }

        return buildResult(additionalItems);
    }

    // -------------------------------------------------------------------------
    //  Helper methods
    // -------------------------------------------------------------------------

    /**
     * Determines whether an invoice represents a one-time purchase.
     * An invoice qualifies when it contains at least one chargeable item and
     * does <b>not</b> contain any {@link InvoiceItemType#RECURRING} items.
     */
    boolean isOneTimePurchaseInvoice(final Invoice invoice) {
        boolean hasCharge = false;
        for (final InvoiceItem item : invoice.getInvoiceItems()) {
            if (item.getInvoiceItemType() == InvoiceItemType.RECURRING) {
                return false;
            }
            if (isChargeableItem(item)) {
                hasCharge = true;
            }
        }
        return hasCharge;
    }

    /**
     * Returns {@code true} when the item type represents a customer charge.
     */
    private boolean isChargeableItem(final InvoiceItem item) {
        return CHARGEABLE_TYPES.contains(item.getInvoiceItemType());
    }

    /**
     * Idempotency check: returns {@code true} if the invoice already contains
     * an adjustment item with the loyalty-discount description.
     */
    private boolean hasExistingLoyaltyDiscount(final Invoice invoice) {
        for (final InvoiceItem item : invoice.getInvoiceItems()) {
            if (InvoiceItemType.ITEM_ADJ.equals(item.getInvoiceItemType())
                    && LOYALTY_DISCOUNT_DESC.equals(item.getDescription())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wraps the list of additional items into an {@link AdditionalItemsResult}.
     */
    private AdditionalItemsResult buildResult(final List<InvoiceItem> additionalItems) {
        return new AdditionalItemsResult() {
            @Override
            public List<InvoiceItem> getAdditionalItems() {
                return additionalItems;
            }

            @Override
            public Iterable<PluginProperty> getAdjustedPluginProperties() {
                return null;
            }
        };
    }

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {
    }
}
