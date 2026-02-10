/*
 * Copyright 2020-2026 The Billing Project, LLC
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.joda.time.LocalDate;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountUserApi;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.invoice.api.InvoiceUserApi;
import org.killbill.billing.invoice.plugin.api.AdditionalItemsResult;
import org.killbill.billing.invoice.plugin.api.InvoiceContext;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.clock.Clock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class HelloWorldInvoicePluginApiTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    private OSGIKillbillAPI killbillAPI;
    private InvoiceUserApi invoiceUserApi;
    private InvoiceContext invoiceContext;
    private HelloWorldInvoicePluginApi pluginApi;

    @BeforeMethod(groups = "fast")
    public void setUp() throws Exception {
        // Build all mocks using doReturn().when() to avoid nested stubbing issues
        final Account account = mock(Account.class);
        doReturn(ACCOUNT_ID).when(account).getId();
        doReturn(Currency.USD).when(account).getCurrency();

        final AccountUserApi accountUserApi = mock(AccountUserApi.class);
        doReturn(account).when(accountUserApi).getAccountById(any(UUID.class), any());

        invoiceUserApi = mock(InvoiceUserApi.class);

        killbillAPI = mock(OSGIKillbillAPI.class);
        doReturn(accountUserApi).when(killbillAPI).getAccountUserApi();
        doReturn(invoiceUserApi).when(killbillAPI).getInvoiceUserApi();

        final OSGIConfigPropertiesService configProperties = mock(OSGIConfigPropertiesService.class);
        final Clock clock = mock(Clock.class);
        invoiceContext = mock(InvoiceContext.class);

        pluginApi = new HelloWorldInvoicePluginApi(killbillAPI, configProperties, clock);
    }

    // ------------------------------------------------------------------
    //  Helpers to build fake invoices & items
    // ------------------------------------------------------------------

    private Invoice buildInvoice(final List<InvoiceItem> items) {
        return buildInvoice(UUID.randomUUID(), items);
    }

    private Invoice buildInvoice(final UUID invoiceId, final List<InvoiceItem> items) {
        final Invoice invoice = mock(Invoice.class);
        doReturn(invoiceId).when(invoice).getId();
        doReturn(ACCOUNT_ID).when(invoice).getAccountId();
        doReturn(new LocalDate()).when(invoice).getInvoiceDate();
        doReturn(items).when(invoice).getInvoiceItems();
        return invoice;
    }

    private InvoiceItem buildItem(final UUID invoiceId, final InvoiceItemType type, final BigDecimal amount) {
        return buildItem(invoiceId, type, amount, null);
    }

    private InvoiceItem buildItem(final UUID invoiceId, final InvoiceItemType type, final BigDecimal amount,
                                  final String description) {
        final InvoiceItem item = mock(InvoiceItem.class);
        doReturn(UUID.randomUUID()).when(item).getId();
        doReturn(invoiceId).when(item).getInvoiceId();
        doReturn(ACCOUNT_ID).when(item).getAccountId();
        doReturn(type).when(item).getInvoiceItemType();
        doReturn(amount).when(item).getAmount();
        doReturn(description).when(item).getDescription();
        doReturn(new LocalDate()).when(item).getStartDate();
        doReturn(new LocalDate()).when(item).getEndDate();
        doReturn(Currency.USD).when(item).getCurrency();
        return item;
    }

    private Invoice buildOneTimePurchaseInvoice(final BigDecimal amount) {
        final UUID invoiceId = UUID.randomUUID();
        final InvoiceItem item = buildItem(invoiceId, InvoiceItemType.EXTERNAL_CHARGE, amount);
        return buildInvoice(invoiceId, Collections.singletonList(item));
    }

    private Invoice buildRecurringInvoice(final BigDecimal amount) {
        final UUID invoiceId = UUID.randomUUID();
        final InvoiceItem item = buildItem(invoiceId, InvoiceItemType.RECURRING, amount);
        return buildInvoice(invoiceId, Collections.singletonList(item));
    }

    private void setPastInvoices(final List<Invoice> pastInvoices) throws Exception {
        doReturn(pastInvoices).when(invoiceUserApi).getInvoicesByAccount(eq(ACCOUNT_ID),
                                                                         eq(false), eq(false), eq(false), any());
    }

    // ------------------------------------------------------------------
    //  Tests
    // ------------------------------------------------------------------

    @Test(groups = "fast")
    public void testNoDiscountOnFirstPurchase() throws Exception {
        // 0 past invoices → purchase #1 → no discount
        setPastInvoices(Collections.<Invoice>emptyList());

        final Invoice newInvoice = buildOneTimePurchaseInvoice(new BigDecimal("100.00"));
        final AdditionalItemsResult result = pluginApi.getAdditionalInvoiceItems(newInvoice, false, Collections.emptyList(), invoiceContext);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.getAdditionalItems().isEmpty(),
                "Expected no discount items for purchase #1");
    }

    @Test(groups = "fast")
    public void testNoDiscountOnSecondPurchase() throws Exception {
        // 1 past one-time invoice → purchase #2 → no discount
        final List<Invoice> past = new ArrayList<Invoice>();
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("50.00")));
        setPastInvoices(past);

        final Invoice newInvoice = buildOneTimePurchaseInvoice(new BigDecimal("100.00"));
        final AdditionalItemsResult result = pluginApi.getAdditionalInvoiceItems(newInvoice, false, Collections.emptyList(), invoiceContext);

        Assert.assertTrue(result.getAdditionalItems().isEmpty(),
                "Expected no discount items for purchase #2");
    }

    @Test(groups = "fast")
    public void testDiscountOnThirdPurchase() throws Exception {
        // 2 past one-time invoices → purchase #3 → 25% discount
        final List<Invoice> past = new ArrayList<Invoice>();
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("50.00")));
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("75.00")));
        setPastInvoices(past);

        final Invoice newInvoice = buildOneTimePurchaseInvoice(new BigDecimal("200.00"));
        final AdditionalItemsResult result = pluginApi.getAdditionalInvoiceItems(newInvoice, false, Collections.emptyList(), invoiceContext);

        final List<InvoiceItem> items = result.getAdditionalItems();
        Assert.assertEquals(items.size(), 1, "Expected exactly 1 discount adjustment item");

        final InvoiceItem adj = items.get(0);
        // 25% of 200 = 50, negated = -50
        Assert.assertEquals(adj.getAmount().compareTo(new BigDecimal("-50.00")), 0,
                "Discount should be -50.00 (25% of 200)");
        Assert.assertEquals(adj.getDescription(), HelloWorldInvoicePluginApi.LOYALTY_DISCOUNT_DESC);
    }

    @Test(groups = "fast")
    public void testNoDiscountOnFourthPurchase() throws Exception {
        // 3 past one-time invoices → purchase #4 → no discount
        final List<Invoice> past = new ArrayList<Invoice>();
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("50.00")));
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("75.00")));
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("200.00")));
        setPastInvoices(past);

        final Invoice newInvoice = buildOneTimePurchaseInvoice(new BigDecimal("120.00"));
        final AdditionalItemsResult result = pluginApi.getAdditionalInvoiceItems(newInvoice, false, Collections.emptyList(), invoiceContext);

        Assert.assertTrue(result.getAdditionalItems().isEmpty(),
                "Expected no discount items for purchase #4");
    }

    @Test(groups = "fast")
    public void testDiscountOnSixthPurchase() throws Exception {
        // 5 past one-time invoices → purchase #6 → 25% discount
        final List<Invoice> past = new ArrayList<Invoice>();
        for (int i = 0; i < 5; i++) {
            past.add(buildOneTimePurchaseInvoice(new BigDecimal("100.00")));
        }
        setPastInvoices(past);

        final Invoice newInvoice = buildOneTimePurchaseInvoice(new BigDecimal("80.00"));
        final AdditionalItemsResult result = pluginApi.getAdditionalInvoiceItems(newInvoice, false, Collections.emptyList(), invoiceContext);

        Assert.assertEquals(result.getAdditionalItems().size(), 1);
        // 25% of 80 = 20
        Assert.assertEquals(result.getAdditionalItems().get(0).getAmount().compareTo(new BigDecimal("-20.00")), 0);
    }

    @Test(groups = "fast")
    public void testRecurringInvoicesNotCountedAsPurchases() throws Exception {
        // 2 past one-time + 3 recurring invoices → only 2 one-time count
        // purchase #3 is this new one-time → discount applies
        final List<Invoice> past = new ArrayList<Invoice>();
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("50.00")));
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("75.00")));
        past.add(buildRecurringInvoice(new BigDecimal("30.00")));
        past.add(buildRecurringInvoice(new BigDecimal("30.00")));
        past.add(buildRecurringInvoice(new BigDecimal("30.00")));
        setPastInvoices(past);

        final Invoice newInvoice = buildOneTimePurchaseInvoice(new BigDecimal("100.00"));
        final AdditionalItemsResult result = pluginApi.getAdditionalInvoiceItems(newInvoice, false, Collections.emptyList(), invoiceContext);

        Assert.assertEquals(result.getAdditionalItems().size(), 1,
                "Recurring invoices should not count as purchases");
        Assert.assertEquals(result.getAdditionalItems().get(0).getAmount().compareTo(new BigDecimal("-25.00")), 0);
    }

    @Test(groups = "fast")
    public void testRecurringInvoiceDoesNotGetDiscount() throws Exception {
        // 2 past one-time invoices → the new invoice is RECURRING → no discount
        final List<Invoice> past = new ArrayList<Invoice>();
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("50.00")));
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("75.00")));
        setPastInvoices(past);

        final Invoice newInvoice = buildRecurringInvoice(new BigDecimal("100.00"));
        final AdditionalItemsResult result = pluginApi.getAdditionalInvoiceItems(newInvoice, false, Collections.emptyList(), invoiceContext);

        Assert.assertTrue(result.getAdditionalItems().isEmpty(),
                "Recurring invoice should not receive a loyalty discount");
    }

    @Test(groups = "fast")
    public void testDiscountOnMultipleLineItems() throws Exception {
        // 2 past invoices → purchase #3, new invoice has 2 charge items
        final List<Invoice> past = new ArrayList<Invoice>();
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("50.00")));
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("75.00")));
        setPastInvoices(past);

        // New invoice with 2 EXTERNAL_CHARGE items
        final UUID newInvoiceId = UUID.randomUUID();
        final InvoiceItem item1 = buildItem(newInvoiceId, InvoiceItemType.EXTERNAL_CHARGE, new BigDecimal("100.00"));
        final InvoiceItem item2 = buildItem(newInvoiceId, InvoiceItemType.FIXED, new BigDecimal("40.00"));
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(item1);
        items.add(item2);
        final Invoice newInvoice = buildInvoice(newInvoiceId, items);

        final AdditionalItemsResult result = pluginApi.getAdditionalInvoiceItems(newInvoice, false, Collections.emptyList(), invoiceContext);

        Assert.assertEquals(result.getAdditionalItems().size(), 2,
                "Expected one discount item per chargeable line item");
        // Item1: 25% of 100 = -25
        Assert.assertEquals(result.getAdditionalItems().get(0).getAmount().compareTo(new BigDecimal("-25.00")), 0);
        // Item2: 25% of 40 = -10
        Assert.assertEquals(result.getAdditionalItems().get(1).getAmount().compareTo(new BigDecimal("-10.00")), 0);
    }

    @Test(groups = "fast")
    public void testIdempotency() throws Exception {
        // 2 past invoices → purchase #3, but the new invoice already has a loyalty discount item
        final List<Invoice> past = new ArrayList<Invoice>();
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("50.00")));
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("75.00")));
        setPastInvoices(past);

        // Build new invoice that already contains a loyalty discount adjustment
        final UUID newInvoiceId = UUID.randomUUID();
        final InvoiceItem chargeItem = buildItem(newInvoiceId, InvoiceItemType.EXTERNAL_CHARGE, new BigDecimal("100.00"));
        final InvoiceItem existingDiscount = buildItem(newInvoiceId, InvoiceItemType.ITEM_ADJ,
                new BigDecimal("-25.00"), HelloWorldInvoicePluginApi.LOYALTY_DISCOUNT_DESC);
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(chargeItem);
        items.add(existingDiscount);
        final Invoice newInvoice = buildInvoice(newInvoiceId, items);

        final AdditionalItemsResult result = pluginApi.getAdditionalInvoiceItems(newInvoice, false, Collections.emptyList(), invoiceContext);

        Assert.assertTrue(result.getAdditionalItems().isEmpty(),
                "Should not add discount items when loyalty discount already exists on invoice");
    }

    @Test(groups = "fast")
    public void testDryRunStillReturnsDiscount() throws Exception {
        // 2 past invoices → purchase #3, dryRun=true → discount items should still be returned
        final List<Invoice> past = new ArrayList<Invoice>();
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("50.00")));
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("75.00")));
        setPastInvoices(past);

        final Invoice newInvoice = buildOneTimePurchaseInvoice(new BigDecimal("200.00"));
        final AdditionalItemsResult result = pluginApi.getAdditionalInvoiceItems(newInvoice, true, Collections.emptyList(), invoiceContext);

        Assert.assertEquals(result.getAdditionalItems().size(), 1,
                "Dry-run should still return discount items for preview");
        Assert.assertEquals(result.getAdditionalItems().get(0).getAmount().compareTo(new BigDecimal("-50.00")), 0);
    }

    @Test(groups = "fast")
    public void testNewInvoiceNotDoubleCountedInPast() throws Exception {
        // Ensure that if the new invoice appears in the pastInvoices list
        // (because getInvoicesByAccountId may include it), it is not double-counted
        final Invoice pastInvoice1 = buildOneTimePurchaseInvoice(new BigDecimal("50.00"));
        final Invoice pastInvoice2 = buildOneTimePurchaseInvoice(new BigDecimal("75.00"));
        final Invoice newInvoice = buildOneTimePurchaseInvoice(new BigDecimal("200.00"));

        // Simulate the new invoice appearing in the past list
        final List<Invoice> pastIncludingNew = new ArrayList<Invoice>();
        pastIncludingNew.add(pastInvoice1);
        pastIncludingNew.add(pastInvoice2);
        pastIncludingNew.add(newInvoice); // new appears in past list
        setPastInvoices(pastIncludingNew);

        final AdditionalItemsResult result = pluginApi.getAdditionalInvoiceItems(newInvoice, false, Collections.emptyList(), invoiceContext);

        // Should still be purchase #3 (2 past + 1 new), not #4
        Assert.assertEquals(result.getAdditionalItems().size(), 1,
                "New invoice should not be double-counted in past invoices");
    }

    @Test(groups = "fast")
    public void testIsOneTimePurchaseInvoice() {
        // One-time charge → true
        final Invoice otInvoice = buildOneTimePurchaseInvoice(new BigDecimal("100.00"));
        Assert.assertTrue(pluginApi.isOneTimePurchaseInvoice(otInvoice));

        // Recurring → false
        final Invoice recInvoice = buildRecurringInvoice(new BigDecimal("100.00"));
        Assert.assertFalse(pluginApi.isOneTimePurchaseInvoice(recInvoice));

        // Empty → false (no chargeable items)
        final Invoice emptyInvoice = buildInvoice(Collections.<InvoiceItem>emptyList());
        Assert.assertFalse(pluginApi.isOneTimePurchaseInvoice(emptyInvoice));

        // Mixed (one-time + recurring) → false (recurring presence disqualifies)
        final UUID mixedId = UUID.randomUUID();
        final List<InvoiceItem> mixedItems = new ArrayList<InvoiceItem>();
        mixedItems.add(buildItem(mixedId, InvoiceItemType.EXTERNAL_CHARGE, new BigDecimal("100.00")));
        mixedItems.add(buildItem(mixedId, InvoiceItemType.RECURRING, new BigDecimal("30.00")));
        final Invoice mixedInvoice = buildInvoice(mixedId, mixedItems);
        Assert.assertFalse(pluginApi.isOneTimePurchaseInvoice(mixedInvoice));
    }

    @Test(groups = "fast")
    public void testZeroAmountItemsSkipped() throws Exception {
        // 2 past invoices → purchase #3, but new invoice charge is $0 → no adjustment
        final List<Invoice> past = new ArrayList<Invoice>();
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("50.00")));
        past.add(buildOneTimePurchaseInvoice(new BigDecimal("75.00")));
        setPastInvoices(past);

        final Invoice newInvoice = buildOneTimePurchaseInvoice(BigDecimal.ZERO);
        final AdditionalItemsResult result = pluginApi.getAdditionalInvoiceItems(newInvoice, false, Collections.emptyList(), invoiceContext);

        Assert.assertTrue(result.getAdditionalItems().isEmpty(),
                "Zero-amount items should not generate discount adjustments");
    }

    @Test(groups = "fast")
    public void testDiscountOnNinthPurchase() throws Exception {
        // 8 past one-time invoices → purchase #9 → discount (9 % 3 == 0)
        final List<Invoice> past = new ArrayList<Invoice>();
        for (int i = 0; i < 8; i++) {
            past.add(buildOneTimePurchaseInvoice(new BigDecimal("100.00")));
        }
        setPastInvoices(past);

        final Invoice newInvoice = buildOneTimePurchaseInvoice(new BigDecimal("60.00"));
        final AdditionalItemsResult result = pluginApi.getAdditionalInvoiceItems(newInvoice, false, Collections.emptyList(), invoiceContext);

        Assert.assertEquals(result.getAdditionalItems().size(), 1);
        // 25% of 60 = 15
        Assert.assertEquals(result.getAdditionalItems().get(0).getAmount().compareTo(new BigDecimal("-15.00")), 0);
    }
}
