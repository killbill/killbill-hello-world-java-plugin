killbill-hello-world-java-plugin
================================

Hello World Kill Bill plugin in Java. It shows how to:

* Build an OSGI plugin using Maven
* Listen to Kill Bill events
* Call Kill Bill APIs from the plugin
* Register a custom HTTP servlet

To build, run `mvn clean install`. You can then install the plugin (`target/hello-world-plugin-*.jar`) in `/var/tmp/bundles/plugins/java/hello/1.0`.
