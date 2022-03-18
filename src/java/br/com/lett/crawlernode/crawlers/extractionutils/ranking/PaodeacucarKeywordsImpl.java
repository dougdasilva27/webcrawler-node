package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PaodeacucarKeywordsImpl extends LinxImpulseRanking {
   public PaodeacucarKeywordsImpl(Session session) {
      super(session);
   }

   @Override
   protected String crawlInternalId(JSONObject product, String internalPid) {String internalId = internalPid;
      String url = product.optString("url");
      Pattern pattern = Pattern.compile("/([0-9]+)");
      Matcher matcher = pattern.matcher(url);
      if (matcher.find()) {
         internalId = matcher.group(1);
      }
      return internalId;
   }


}
