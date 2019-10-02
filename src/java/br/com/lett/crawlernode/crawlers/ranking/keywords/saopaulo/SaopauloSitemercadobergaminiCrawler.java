package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

public class SaopauloSitemercadobergaminiCrawler extends BrasilSitemercadoCrawler {
  public SaopauloSitemercadobergaminiCrawler(Session session) {
    super(session);
  }

  public static final String HOME_PAGE =
      "https://www.sitemercado.com.br/bergamini/sao-paulo-hiper-vila-constanca-avenida-luis-stamatis/";
  public static final String LOAD_PAYLOAD =
      "{\"lojaUrl\":\"sao-paulo-hiper-vila-constanca-avenida-luis-stamatis\",\"redeUrl\":\"bergamini\"}";

  @Override
  protected String getHomePage() {
    return HOME_PAGE;
  }

  @Override
  protected String getLoadPayload() {
    return LOAD_PAYLOAD;
  }
}
