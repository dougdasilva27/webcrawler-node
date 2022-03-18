package br.com.lett.crawlernode.database;

import br.com.lett.crawlernode.util.MathUtils;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
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


   public static void reportPriceChanges(Session session, String author, String avatar, String quote, float discount, Float previousPrice, Float newPrice, String originalName, String url, String imageUrl) {

      try {


         StringBuilder msg = new StringBuilder();
         msg.append(quote);
         msg.append("\n");
         msg.append("**");
         msg.append(MathUtils.normalizeTwoDecimalPlaces(discount));
         msg.append("% OFF**");

         StringBuilder msgDescription = new StringBuilder();
         msgDescription.append("De: ~~R$");
         msgDescription.append(MathUtils.normalizeTwoDecimalPlaces(previousPrice));
         msgDescription.append("~~ Por: R$");
         msgDescription.append(MathUtils.normalizeTwoDecimalPlaces(newPrice));



         WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder();

         WebhookEmbed.EmbedTitle title = new WebhookEmbed.EmbedTitle(originalName, url);
         embedBuilder.setTitle(title);
         embedBuilder.setDescription(msgDescription.toString());
         WebhookEmbed.EmbedAuthor authorEmbed = new WebhookEmbed.EmbedAuthor(session.getMarket().getFullName(),null, null);
         embedBuilder.setAuthor(authorEmbed);
         embedBuilder.setThumbnailUrl(imageUrl);


         WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder();
         messageBuilder.setUsername(author);
         messageBuilder.setAvatarUrl(avatar);
         messageBuilder.setContent(msg.toString());
         messageBuilder.addEmbeds(embedBuilder.build());
         DISCORD_CLIENT.send(messageBuilder.build());
      } catch (Exception ex) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(ex));
      }
   }
}
