package br.com.lett.crawlernode.crawlers.ranking.keywords.recife;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

import java.util.Arrays;

public class RecifeRappiatacadaoCrawler extends RappiCrawler {
   public RecifeRappiatacadaoCrawler(Session session) {
      super(session, Arrays.asList("900159163"));
   }
}
