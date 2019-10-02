package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

public class BrasilCsdCrawler extends BrasilSitemercadoCrawler {

  public BrasilCsdCrawler(Session session) {
    super(session);
  }

  public static final String HOME_PAGE =
      "https://www.sitemercado.com.br/supermercadoscidadecancao/londrina-loja-londrina-19-rodocentro-avenida-tiradentes/";
  public static final String LOAD_PAYLOAD =
      "{\"lojaUrl\":\"londrina-loja-londrina-19-rodocentro-avenida-tiradentes\",\"redeUrl\":\"supermercadoscidadecancao\"}";

  @Override
  protected String getHomePage() {
    return HOME_PAGE;
  }

  @Override
  protected String getLoadPayload() {
    return LOAD_PAYLOAD;
  }
}
