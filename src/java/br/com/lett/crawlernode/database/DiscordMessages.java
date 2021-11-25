package br.com.lett.crawlernode.database;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

/**
 * Send price change to Discord
 */
public class DiscordMessages {

   private static final WebhookClient DISCORD_CLIENT = WebhookClient.withUrl("https://discord.com/api/webhooks/913473681945133116/DP5i3656CaeZpkevRtu193lTgjuFzfny1EwwltfXRJaK5OS374upqfWGK1sGzbgtE4Rq");

   private DiscordMessages() {
   }

   private static final Logger logger = LoggerFactory.getLogger(DiscordMessages.class);

   public static void reportPriceChanges(Session session, String msg, String author, String avatar) {
      try {

         WebhookMessageBuilder builder = new WebhookMessageBuilder();
         builder.setUsername(author);
         builder.setAvatarUrl(avatar);
         builder.setContent(msg);
         DISCORD_CLIENT.send(builder.build());
      } catch (Exception ex) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(ex));
      }
   }

}
