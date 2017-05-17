package br.com.lett.crawlernode.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackAttachment;
import net.gpedro.integrations.slack.SlackField;
import net.gpedro.integrations.slack.SlackMessage;

/**
 * Classe onde são reportados os erros para o canal no slack
 */

public class DBSlack {

	private static Logger logger = LoggerFactory.getLogger(DBSlack.class);

	public static void reportErrorRanking(String slack_user, String text, String fields, String keyword, int market) {
		try {
			SlackApi api = new SlackApi("https://hooks.slack.com/services/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");
			SlackMessage message = new SlackMessage("@gabrieldornelas", slack_user, "");
			
			String textFinal = "\n*Keyword*: " + keyword + " *Market*: " + market + 
					"\n\n" + text + "\n -----------------------------------------------------------------------------------------";
			
			SlackAttachment attach = new SlackAttachment();
			attach.setAuthorName("RANKING DATA ALERT");
			attach.setThumbUrl("http://vignette4.wikia.nocookie.net/the-mary-sue/images/c/cd/Warning_sign.png/revision/latest?cb=20160106153816");
			attach.setTitle("CRAWLER RANKING KEYWORDS");
			attach.setColor("warning");
			attach.setPretext("<@gabrieldornelas>");
			attach.setText(textFinal);
			attach.addMarkdownAttribute("text");
			attach.addMarkdownAttribute("pretext");
			attach.addMarkdownAttribute("fields");

			// Adicionando Informações adicionais
			if(fields != null) {
				SlackField info = new SlackField();
				info.setTitle("");
				info.setValue(fields);
				attach.addFields(info);
			}

			attach.setFallback("RANKING DATA ALERT");
			message.addAttachments(attach);

			api.call(message);
			Logging.printLogDebug(logger, "Send one message to slack.");
		} catch(Exception ex) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(ex));
		}
	}
}
