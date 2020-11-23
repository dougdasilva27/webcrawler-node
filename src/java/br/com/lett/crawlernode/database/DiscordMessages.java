package br.com.lett.crawlernode.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mrpowergamerbr.temmiewebhook.DiscordMessage;
import com.mrpowergamerbr.temmiewebhook.TemmieWebhook;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

/**
 * Classe onde são reportados os erros para o canal no slack
 */

public class DiscordMessages {

   private static Logger logger = LoggerFactory.getLogger(DiscordMessages.class);

   public static void reportPriceChanges(Session session, String msg, String author, String avatar) {
      try {
         TemmieWebhook temmie = new TemmieWebhook("https://discord.com/api/webhooks/780417668830330922/U7ns_RyD1qhwpeysi46hNhbdLQP9fdB7aEftAkBZdrHxR4JIxsQoKfbO3tBI4LxU53RL");
         DiscordMessage dm = new DiscordMessage();
         dm.setUsername(author);
         dm.setAvatarUrl(avatar);
         dm.setContent(msg);
         temmie.sendMessage(dm);
         Logging.printLogDebug(logger, session, "Send one message to Discord.");
      } catch (Exception ex) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(ex));
      }
   }

}
