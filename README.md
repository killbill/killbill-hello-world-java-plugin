# killbill-hello-world-java-plugin

Hello World Kill Bill plugin in Java. It shows how to:

* Build an OSGI plugin using Maven
* Listen to Kill Bill events
* Call Kill Bill APIs from the plugin
* Register a custom HTTP servlet

## Getting Started

To build, run `mvn clean install`. You can then install the plugin locally:

```
kpm install_java_plugin helloworld --from-source-file target/hello-world-plugin-*-SNAPSHOT.jar --destination /var/tmp/bundles
```

You can also use it as a template for your own plugins:

```bash
curl https://codeload.github.com/killbill/killbill-hello-world-java-plugin/tar.gz/master | tar zxvf - --strip-components=1
rm -rf .circleci LICENSE .idea/copyright

PACKAGE=acme
PREFIX=Acme

mv src/main/java/org/killbill/billing/plugin/helloworld src/main/java/org/killbill/billing/plugin/$PACKAGE
find . -name 'HelloWorld*.java' -exec bash -c 'mv $0 ${0/HelloWorld/'$PREFIX'}' {} \;

find pom.xml src -type f -print0 | xargs -0 sed -i '' 's/org\.killbill\.billing\.plugin\.helloworld/org\.killbill\.billing\.plugin\.'$PACKAGE'/g'
find pom.xml src -type f -print0 | xargs -0 sed -i '' 's/HelloWorld/'$PREFIX'/g'
find pom.xml src -type f -print0 | xargs -0 sed -i '' 's/helloWorld/'$PACKAGE'/g'
find .idea pom.xml src -type f -print0 | xargs -0 sed -i '' 's/hello-world-/'$PACKAGE'-/g'
```

Finally, modify the pom.xml with your own Git urls.

## About

Kill Bill is the leading Open-Source Subscription Billing & Payments Platform. For more information about the project, go to https://killbill.io/.
