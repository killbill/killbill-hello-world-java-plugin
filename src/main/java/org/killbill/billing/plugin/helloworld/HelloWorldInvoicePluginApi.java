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

class HelloWorldInvoicePluginApi extends PluginInvoicePluginApi  implements OSGIKillbillEventHandler {
	
	public HelloWorldInvoicePluginApi(OSGIKillbillAPI killbillAPI, OSGIConfigPropertiesService configProperties, Clock clock) {
		super(killbillAPI, configProperties, clock);
	}

	@Override
	public List<InvoiceItem> getAdditionalInvoiceItems(final Invoice newInvoice, final boolean dryRun, 
			final Iterable<PluginProperty> properties, final CallContext callContext) {
		ImmutableList.Builder<InvoiceItem> additionalItems  = ImmutableList.builder();
		List<InvoiceItem> invoiceItems = newInvoice.getInvoiceItems();
		InvoiceItem newItem = null;
		BigDecimal charge=new BigDecimal("100");
        if(invoiceItems.size()>0) {
        	InvoiceItem invoiceItem = invoiceItems.get(0);
        	newItem  = PluginInvoiceItem.createTaxItem(invoiceItem, invoiceItem.getInvoiceId(),newInvoice.getInvoiceDate(), null ,charge, "Tax Item");
        }
        additionalItems.add(newItem);
		return additionalItems.build();
	}
	
	@Override
	public void handleKillbillEvent(ExtBusEvent killbillEvent) {}

}