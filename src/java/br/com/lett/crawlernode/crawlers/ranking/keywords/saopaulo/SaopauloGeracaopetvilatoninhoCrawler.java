package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.GeracaopetCrawler;

public class SaopauloGeracaopetvilatoninhoCrawler extends GeracaopetCrawler {

  private static final String CEP = "15081-500";

  public SaopauloGeracaopetvilatoninhoCrawler(Session session) {
    super(session, CEP);
  }

}
