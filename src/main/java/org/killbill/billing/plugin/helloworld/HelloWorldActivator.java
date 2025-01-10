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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.xml.namespace.QName;
import org.killbill.billing.invoice.plugin.api.InvoiceFormatterFactory;
import org.killbill.billing.invoice.plugin.api.InvoicePluginApi;
import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIFrameworkEventHandler;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.plugin.api.notification.PluginConfigurationEventHandler;
import org.killbill.billing.plugin.core.config.PluginEnvironmentConfig;
import org.killbill.billing.plugin.core.resources.jooby.PluginApp;
import org.killbill.billing.plugin.core.resources.jooby.PluginAppBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPBinding;

public class HelloWorldActivator extends KillbillActivatorBase {

    //
    // Ideally that string should match the pluginName on the filesystem, but there
    // is no enforcement
    //
    public static final String PLUGIN_NAME = "hello-world-plugin";

    private HelloWorldConfigurationHandler helloWorldConfigurationHandler;
    private OSGIKillbillEventDispatcher.OSGIKillbillEventHandler killbillEventHandler;
    private MetricsGeneratorExample metricsGenerator;

    private ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> invoiceFormatterTracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final String region = PluginEnvironmentConfig.getRegion(configProperties.getProperties());

        // Register an event listener for plugin configuration (optional)
        helloWorldConfigurationHandler = new HelloWorldConfigurationHandler(region, PLUGIN_NAME, killbillAPI);
        final Properties globalConfiguration = helloWorldConfigurationHandler
                .createConfigurable(configProperties.getProperties());
        helloWorldConfigurationHandler.setDefaultConfigurable(globalConfiguration);

        // create a service tracker for a custom InvoiceFormatter service
        invoiceFormatterTracker = new ServiceTracker<>(context, InvoiceFormatterFactory.class, null);
        invoiceFormatterTracker.open();


        // Register an event listener (optional)
        killbillEventHandler = new HelloWorldListener(killbillAPI, invoiceFormatterTracker, configProperties.getProperties());

        // As an example, this plugin registers a PaymentPluginApi (this could be
        // changed to any other plugin api)
        final PaymentPluginApi paymentPluginApi = new HelloWorldPaymentPluginApi();
        registerPaymentPluginApi(context, paymentPluginApi);

        // Expose metrics (optional)
        metricsGenerator = new MetricsGeneratorExample(metricRegistry);
        metricsGenerator.start();

        // Expose a healthcheck (optional), so other plugins can check on the plugin status
        final Healthcheck healthcheck = new HelloWorldHealthcheck();
        registerHealthcheck(context, healthcheck);

        // This Plugin registers a InvoicePluginApi
        final InvoicePluginApi invoicePluginApi = new HelloWorldInvoicePluginApi(killbillAPI, configProperties, null);
        registerInvoicePluginApi(context, invoicePluginApi);

        // Register a servlet (optional)
        final PluginApp pluginApp = new PluginAppBuilder(PLUGIN_NAME, killbillAPI, dataSource, super.clock,
                                                         configProperties).withRouteClass(HelloWorldServlet.class)
                                                                          .withRouteClass(HelloWorldHealthcheckServlet.class).withService(healthcheck).build();
        final HttpServlet httpServlet = PluginApp.createServlet(pluginApp);
        registerServlet(context, httpServlet);

        registerHandlers();

        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            soap();
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private void soap() throws SOAPException, IOException {
        String endpointAddress = "http://www.dneonline.com/calculator.asmx";
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage request = factory.createMessage();
        SOAPPart soapPart = request.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        SOAPBody body = envelope.getBody();
        SOAPElement operation = body.addChildElement("Add", "", "http://tempuri.org/");
        operation.addChildElement("intA").addTextNode("5");
        operation.addChildElement("intB").addTextNode("7");
        request.saveChanges();
        QName serviceName = new QName("http://tempuri.org/", "Calculator");
        QName portName    = new QName("http://tempuri.org/", "CalculatorSoap");
        Service service = Service.create(serviceName);
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, endpointAddress);
        Dispatch<SOAPMessage> dispatch = service.createDispatch(portName, SOAPMessage.class, Service.Mode.MESSAGE);
        System.out.println("Sending 'Add' request to the public Calculator service...");
        SOAPMessage response = dispatch.invoke(request);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        response.writeTo(out);
        System.out.println("\nSOAP Response:\n" + new String(out.toByteArray()));
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        // Do additional work on shutdown (optional)
        metricsGenerator.stop();
        super.stop(context);
    }

    private void registerHandlers() {
        final PluginConfigurationEventHandler configHandler = new PluginConfigurationEventHandler(
                helloWorldConfigurationHandler);

        dispatcher.registerEventHandlers(configHandler,
                                         (OSGIFrameworkEventHandler) () -> dispatcher.registerEventHandlers(killbillEventHandler));
    }

    private void registerServlet(final BundleContext context, final Servlet servlet) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Servlet.class, servlet, props);
    }

    private void registerPaymentPluginApi(final BundleContext context, final PaymentPluginApi api) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, PaymentPluginApi.class, api, props);
    }

    private void registerInvoicePluginApi(final BundleContext context, final InvoicePluginApi api) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, InvoicePluginApi.class, api, props);
    }

    private void registerHealthcheck(final BundleContext context, final Healthcheck healthcheck) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Healthcheck.class, healthcheck, props);
    }
}
