package br.com.lett.crawlernode.core.server.endpoints;

import br.com.lett.crawlernode.core.MicrometerExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class MetricsEndpoint extends HttpServlet {

   protected static final Logger logger = LoggerFactory.getLogger(MetricsEndpoint.class);

   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
      String response = MicrometerExporter.getInstance().getRegistry().scrape();

      resp.setStatus(200);

      try (PrintWriter pw = resp.getWriter()) {
         pw.write(response);
      } catch (IOException exception) {
         logger.error("Could not send metrics");
      }
   }
}
