package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.GeracaopetCrawler;

public class SaopauloGeracaopetvilatoninhoCrawler extends GeracaopetCrawler {

  private static final String CEP = "15081-500";

  public SaopauloGeracaopetvilatoninhoCrawler(Session session) {
    super(session, CEP);
  }

}
