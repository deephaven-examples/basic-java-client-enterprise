# Core+ Java Client Example

This repository contains an example Deephaven Core+ Java client application, for use with a Deephaven Enterprise
installation. The `build.gradle` file includes all the required dependencies. Be sure to update the versions in the
`gradle.properties` file to match the version of your Deephaven installation — the version numbers specified in
`gradle.properties` determine which version of Deephaven libraries are retrieved by Gradle.

The example class in this repository (`org.example.ClientExample`) connects and authenticates to a Deephaven cluster (
using either a username and password or a private key file for authentication), then starts a new query worker to run a
basic operation and retrieve its result. It also demonstrates connecting to an existing persistent query and retrieving
a snapshot of one of the persistent query's tables.

This example serves to demonstrate connecting and authenticating to a Deephaven cluster and obtaining API connections to
both new workers and existing persistent queries, but does not cover the extensive functionality of the client API. For
further examples of operations supported with Barrage and the Deephaven APIs, please see the example in the
[basic-java-client](https://github.com/deephaven-examples/basic-java-client) repository.

## Running the example program

The example application can be run using Gradle, which will set the required JVM arguments. To start the example
using a username and password for authentication, run the following command:

```shell
./gradlew run --args="https://<Deephaven server address>:<Deephaven port> <username>:<password>"
```

To retrieve a snapshot of a table from a persistent query, include the `<query name>` and `<table name>` arguments as in
the following example:

```shell
./gradlew run --args="https://<Deephaven server address>:<Deephaven port> <username>:<password> <query name> <table name>"
```

Instead of providing a username and password, the example can be run
with [private key authentication](https://deephaven.io/enterprise/docs/sys-admin/configuration/public-and-private-keys/#user-private-keyfile)
by specifying a path to a private key file in place of the `<username>:<password>` argument, as in the examples below. (
For more information on creating and adding private keys,
see [How to Create Deephaven Authentication Keys](https://deephaven.io/enterprise/docs/development/authentication/#how-to-create-deephaven-authentication-keys).)

```shell
./gradlew run --args="<Deephaven server address>:<Deephaven port> </path/to/keyfile> <query name> <table name>"
```

```shell
./gradlew run --args="https://deephaven.mycompany.net:8123 /home/myusername/my-dh-private-key.txt MyTestQuery table_123"
```

## JVM arguments

When creating your own clients, be sure to set the following JVM arguments:

```
-DConfiguration.rootFile=dh-defaults.prop
-DDeephavenEnterprise.rootFile=iris-common.prop
--add-opens=java.base/java.nio=ALL-UNNAMED org.example.ClientExample
```

- The `-DConfiguration.rootFile` property is used when initializing the application. This is included in the Deephaven
  dependencies.
- The `-DDeephavenEnterprise.rootFile` property specifies the Deephaven property file to retrieve from the configuration
  server, after the client opens its initial connection to the Deephaven server and authenticates.
- The `--add-opens` option is a JVM argument required to enable the use of certain required Java APIs by reflection.

For example, when running your own Deephaven client, the command will look similar to the following:

```shell
java -classpath <classpath> -DDeephavenEnterprise.rootFile=iris-common.prop -DConfiguration.rootFile=dh-defaults.prop --add-opens=java.base/java.nio=ALL-UNNAMED org.example.ClientExample org.example.ExampleClient https://deephaven-prod.mycompany.net:8123 </path/to/keyfile> <query name> <table name>
```

Please see the [Deephaven Enterprise documentation](https://deephaven.io/enterprise/docs/) or contact Deephaven
support for more information.