package org.killbill.billing.plugin.helloworld;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.api.invoice.PluginInvoiceItem;
import org.killbill.billing.plugin.api.invoice.PluginInvoicePluginApi;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.clock.Clock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

class HelloWorldInvoicePlugn extends PluginInvoicePluginApi  implements OSGIKillbillEventHandler {
	
	public HelloWorldInvoicePlugn(OSGIKillbillAPI killbillAPI, OSGIConfigPropertiesService configProperties, Clock clock) {
		super(killbillAPI, configProperties, clock);
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<InvoiceItem> getAdditionalInvoiceItems(final Invoice newInvoice, final boolean dryRun, 
			final Iterable<PluginProperty> properties, final CallContext callContext) {
		UUID accountId = newInvoice.getAccountId();
        Account account = getAccount(accountId, callContext);
		Set<Invoice> invoices =  allInvoicesOfAccount(account, newInvoice, callContext);
		
		ImmutableList.Builder<InvoiceItem> additionalItems  = ImmutableList.builder();
		for(Invoice invoice : invoices) {
			if (invoice.equals(newInvoice)) {
                ImmutableList.Builder<InvoiceItem> newIntItems = ImmutableList.builder();
                BigDecimal charge=new BigDecimal("100");
                //For generating tax item for each invoice item in new invoice
                for (InvoiceItem item : newInvoice.getInvoiceItems()) {
                	InvoiceItem newItem  = PluginInvoiceItem.createTaxItem(item, item.getInvoiceId(),invoice.getInvoiceDate(), null ,charge, "Tax Item");
                	newIntItems.add(newItem);
                }
                additionalItems.addAll(newIntItems.build());
            }
		}
		return additionalItems.build();
	}

	private Set<Invoice> allInvoicesOfAccount(Account account, Invoice newInvoice, TenantContext tenantCtx) {
        ImmutableSet.Builder<Invoice> builder = ImmutableSet.builder();
        builder.addAll(getInvoicesByAccountId(account.getId(), tenantCtx));
        builder.add(newInvoice);
        return builder.build();
    }

	@Override
	public void handleKillbillEvent(ExtBusEvent killbillEvent) {
		// TODO Auto-generated method stub		
	}

}