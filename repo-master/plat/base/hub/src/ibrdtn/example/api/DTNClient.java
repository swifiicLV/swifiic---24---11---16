package ibrdtn.example.api;

import ibrdtn.api.APIException;
import ibrdtn.api.EventClient;
import ibrdtn.api.ExtendedClient;
import ibrdtn.api.object.Bundle;
import in.swifiic.plat.helper.hub.SwifiicHandler;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;

/**
 * A thin wrapper around the IBR-DTN Java Library that takes care of connecting to the daemon and provides some
 * convenience functions.
 *
 * @author Julian Timpner <timpner@ibr.cs.tu-bs.de>
 */
public class DTNClient {

    //private static final Logger logger = Logger.getLogger(DTNClient.class.getName());
     private LogExample logger ;
    private ExecutorService executor;
    private ExtendedClient exClient = null;
    private EventClient eventClient = null;
    private ibrdtn.api.sab.CallbackHandler sabHandler = null;
    private String endpoint = null;
    private SwifiicHandler hndlr = null;

    /**
     * Constructor allowing to choose between different payload types and API handling strategies.
     *
     * @param endpoint the application's primary EID
     * @param type the expected payload format
     */
    public DTNClient(String endpoint, SwifiicHandler handler) {
        executor = Executors.newSingleThreadExecutor();

        this.endpoint = endpoint;
        hndlr = handler;
	  logger = new LogExample() ;
        exClient = new ExtendedClient();

        sabHandler = new AbstractAPIHandler(exClient, executor, hndlr);    
        exClient.setHandler(sabHandler);
        exClient.setHost(Constants.HOST);
        exClient.setPort(Constants.PORT);
        

        connect();
    }

    /**
     * Opens the API connection to the DTN daemon.
     */
    private void connect() {
        try {
            exClient.open();
            logger.logExample("(from IBRAPI) Successfully connected to DTN daemon");

            exClient.setEndpoint(endpoint);
            logger.logExample("(from IBRAPI) Endpoint ''+endpoint+'' registered.");

        } catch (APIException e) {
            logger.logExample("(from IBRAPI) API error:" +e.getMessage());
        } catch (IOException e) {
            logger.logExample("(from IBRAPI) Could not connect to DTN daemon:" + e.getMessage());
        }
    }

    /**
     * Reconnect  to service if prior error
     */
    public void reconnect() {
    	try {
    		this.exClient.close();
    		exClient = null;
    		System.gc();
    		Thread.sleep(1000); // delay for a second
    		
    		exClient = new ExtendedClient();

            sabHandler = new AbstractAPIHandler(exClient, executor, hndlr);    
            exClient.setHandler(sabHandler);
            exClient.setHost(Constants.HOST);
            exClient.setPort(Constants.PORT);
    		
    		connect();
    	} catch (Exception ex) {
    		logger.logExample("(from IBRAPI) Could not reconnect to Client " + ex.getLocalizedMessage());
    	}
    	
    }
    /**
     * Sends the given Bundle to the daemon.
     *
     * @param bundle
     */
    public void send(Bundle bundle) {

        logger.logExample("(from IBRAPI) Sending " + bundle);

        final Bundle finalBundle = bundle;
        if(! exClient.isConnected()) {
        	logger.logExample("(from IBRAPI) During send found client as disconnected - attempting reconnect");
        	reconnect();
        }

        final ExtendedClient finalClient = this.exClient;

        executor.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    finalClient.send(finalBundle);
                } catch (Exception e) {
                    logger.logExample("(from IBRAPI) Unable to send bundle" +  e);
                }
            }
        });
    }

    /**
     * Shuts down the API connection.
     */
    public void shutdown() {

        logger.logExample("(from IBRAPI) Shutting down" +endpoint);

        executor.shutdown(); // Disable new tasks from being submitted
        try {

            if (!executor.awaitTermination(8, TimeUnit.SECONDS)) {

                logger.logExample("(from IBRAPI) Thread pool did not terminate on shutdown");

                executor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.logExample("Thread pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {

            logger.logExample("(from IBRAPI) Shutdown interrupted: {0}" +ie.getMessage());
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

        try {
            exClient.close();
            if (eventClient != null) {
                eventClient.close();
            }
        } catch (IOException e) {
            logger.logExample("(from IBRAPI) Could not close connection to the DTN daemon:" +e.getMessage());
        }

        logger.logExample("(from IBRAPI) DTN connection closed");
    }

    /**
     * Enables/disables event notifications from the daemon.
     *
     * These are different from (custody) reports, which are received anyway.
     *
     * @param eventNotifications true if event notifications are to be enabled
     */
    public void setEvents(boolean eventNotifications) {
        if (eventNotifications) {
            EventNotifier notifier = new EventNotifier();
            eventClient = new EventClient(notifier);
            try {
                eventClient.open();
            } catch (IOException e) {
                logger.logExample("(from IBRAPI) Could not connect to DTN daemon:" +e.getMessage());
            }
        } else {
            if (eventClient != null) {
                try {
                    eventClient.close();
                } catch (IOException e) {
                    logger.logExample("(from IBRAPI) Closing EventClient connection failed: " +e.getMessage());
                }
            }
        }
    }

    public String getConfiguration() {
        return "DTN Client: " + endpoint + " [" + Constants.HOST + ":" + Constants.PORT + "]";
    }

    public ExtendedClient getEC() {
        return this.exClient;
    }
}
