package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

import java.util.Arrays;

public class SaopauloRappiatacadaoipirangaCrawler extends RappiCrawler {
   public SaopauloRappiatacadaoipirangaCrawler(Session session) {
      super(session, Arrays.asList("900159165"));
   }

}
