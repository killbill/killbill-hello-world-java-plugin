killbill-hello-world-java-plugin
================================

Hello World Kill Bill plugin in Java. It shows how to:

* Build an OSGI plugin using Maven
* Listen to Kill Bill events
* Call Kill Bill APIs from the plugin
* Register a custom HTTP servlet

To build, run `mvn clean install`. You can then install the plugin (`target/killbill-osgi-bundles-hello-world-*.jar`) in `/var/tmp/bundles/platform`.
