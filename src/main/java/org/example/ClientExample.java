package org.example;

import io.deephaven.client.impl.SessionImpl;
import io.deephaven.client.impl.TableHandle;
import io.deephaven.engine.table.Table;
import io.deephaven.engine.util.TableTools;
import io.deephaven.enterprise.config.HttpUrlPropertyInputStreamLoader;
import io.deephaven.enterprise.dnd.client.DndSession;
import io.deephaven.enterprise.dnd.client.DndSessionBarrage;
import io.deephaven.enterprise.dnd.client.DndSessionFactoryBarrage;
import io.deephaven.enterprise.dnd.client.exception.SnapshotException;
import io.deephaven.qst.table.TableSpec;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is an example of a Java client application that connects to a Deephaven Enterprise system. Please see
 * the README.md file for more information.
 */
public class ClientExample {
    public static void main(String[] args) throws IOException {
        if (args.length != 2 && args.length != 4) {
            System.out.println("Usage:\n" +
                    "java " + ClientExample.class.getName() + " <server URL> <username>:<password> [query name] [table name]\n" +
                    "java " + ClientExample.class.getName() + " <server URL> <private-key-path> [query name] [table name]\n" +
                    "\nExamples:\n" +
                    "java " + ClientExample.class.getName() + " https://deephaven.mycompany.net:8123 iris:iris\n" +
                    "java " + ClientExample.class.getName() + " https://deephaven.mycompany.net:8123 /home/myusername/my-dh-private-key.txt \n" +
                    "java " + ClientExample.class.getName() + " https://deephaven.mycompany.net:8123 iris:iris MyPersistentQuery my_example_table"
            );
            System.exit(1);
        }


        final String serverUrl = args[0];

        // Configure the Configuration library to load configuration files via HTTP from the specified URL
        HttpUrlPropertyInputStreamLoader.setServerUrl(serverUrl);

        // Create the session factory for Barrage. This creates the connections to the server and downloads the
        // configuration.
        final DndSessionFactoryBarrage sessionFactory = new DndSessionFactoryBarrage(serverUrl + "/iris/connection.json");

        // Parse either the username/password or the private key file from the arguments
        final String authArg = args[1];
        final Pattern authArgPattern = Pattern.compile("(.+?):(.+)");
        final Matcher m = authArgPattern.matcher(authArg);
        if (m.matches()) {
            // Argument is <username>:<password>, so use password authentication
            final String username = m.group(1);
            final String password = m.group(2);
            sessionFactory.password(username, password);
        } else {
            // Authenticate using a private key file. (Assume that 'authArg' is the path to a private key file.)
            sessionFactory.privateKey(authArg);
        }

        final String queryName, tableName;
        if (args.length > 2) {
            queryName = args[2];
            tableName = args[3];
        } else {
            queryName = null;
            tableName = null;
        }

        try {
            createAndRetrieveTableFromNewWorker(sessionFactory);

            if (queryName != null && tableName != null) {
                retrieveTableFromPersistentQuery(sessionFactory, queryName, tableName);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * Start a new Deephaven worker, create an example table using the client API, then pull a snapshot of that table
     * back to the client (i.e. this application). The retrieved table is printed to STDOUT with {@link TableTools#show}.
     *
     * @param sessionFactory The session factory to use to create the new worker.
     * @throws IOException
     * @throws TableHandle.TableHandleException
     * @throws InterruptedException
     * @throws SnapshotException
     */
    public static void createAndRetrieveTableFromNewWorker(DndSessionFactoryBarrage sessionFactory) throws IOException, TableHandle.TableHandleException, InterruptedException, SnapshotException {
        final DndSessionBarrage barrageSession = sessionFactory.newWorker(
                "ExampleTestWorker",
                2,
                600_000,
                10_000);

        try (final SessionImpl clientSession = barrageSession.session()) {
            final TableHandle myNewTable = clientSession.execute(TableSpec.empty(100).update("RowIdx=ii", "MyCol = randomDouble(0, 1000)"));

            final Table localCopyOfTable = barrageSession.snapshotOf(myNewTable.table());

            System.out.println("Printing new table from test worker:");
            TableTools.show(localCopyOfTable);
        } finally {
            barrageSession.close();
        }

    }

    /**
     * Connect to a running persistent query (named {@code queryName}) and retrieve the specified table
     * (given by {@code tableName}). A snapshot of the table is pulled back to the client (i.e. this application) and
     * printed to STDOUT with {@link TableTools#show}.
     *
     * @param sessionFactory The session factory to use to create the connection to the persistent query
     * @param queryName      The name of the persistent query to connect to. This persistent query must be created previously
     *                       and must currently be running.
     * @param tableName      The name of the table to retrieve from the persistent query.
     * @throws SnapshotException
     * @throws IOException
     */
    public static void retrieveTableFromPersistentQuery(DndSessionFactoryBarrage sessionFactory, String queryName, String tableName) throws SnapshotException, IOException {
        final DndSessionBarrage clientSession = sessionFactory.persistentQuery(queryName);
        try {
            final Table localCopyOfTable = clientSession.snapshotOf(DndSession.scopeTable(tableName).tail(100));

            System.out.println("Printing table " + tableName + " from query " + queryName + ": ");
            TableTools.show(localCopyOfTable);

        } finally {
            clientSession.close();
        }
    }


}