package br.com.lett.crawlernode.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.aws.kinesis.KPLProducer;
import br.com.lett.crawlernode.aws.sqs.QueueHandler;
import br.com.lett.crawlernode.core.server.Server;
import br.com.lett.crawlernode.core.task.Resources;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.util.Logging;


/**
 * Environment variables: DEBUG : to print debug log messages on console [ON/OFF] ENVIRONMENT
 * [development, production]
 *
 * <p>
 * Environments:
 * </p>
 * <ul>
 * <li>development: in this mode we trigger the program by the Test class and it's main method. This
 * is the fastest and basic mode for testing. It only tests the crawler information extraction
 * logic.</li>
 * <li>production: in this mode we run the Main class and starts the crawler server. It will keep
 * listening on the task endpoint, under port 5000. To run any task, the user must assemble a POST
 * request (you can use Postman) and send the POST for the server. Running this way, all the process
 * will run (data will be stored in database and all postprocessing will take place after the main
 * information is crawled.)</li>
 * </ul>
 *
 * @author Samir Leão
 */

public class Main {

   private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

   public static QueueHandler queueHandler;
   public static Resources globalResources;
   public static Server server;

   public static void main(String[] args) {
      Logging.printLogInfo(LOGGER, "Starting webcrawler-node...");

      // Setting global configuraions
      GlobalConfigurations.setConfigurations();

      // Create Kinesis KPL child process
      KPLProducer.getInstance();

      // Check resources
      Logging.printLogInfo(LOGGER, "Checking files...");
      // Initialize temporary folder for images download
      Persistence.initializeImagesDirectories(GlobalConfigurations.markets);

      // Create a queue handler that will contain an Amazon SQS instance
      queueHandler = new QueueHandler();

      // Create the server
      server = new Server();
   }
}
