package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author gabriel date: 2018-03-15
 */
public class BrasilCsdCrawler extends BrasilSitemercadoCrawler {

  public BrasilCsdCrawler(Session session) {
    super(session);
  }

  public static final String HOME_PAGE =
          "https://www.sitemercado.com.br/supermercadoscidadecancao/londrina-loja-londrina-19-rodocentro-avenida-tiradentes/";
  public static final String LOAD_PAYLOAD =
          "{\"lojaUrl\":\"londrina-loja-londrina-19-rodocentro-avenida-tiradentes\",\"redeUrl\":\"supermercadoscidadecancao\"}";

  public static final int IDLOJA = 503;
  public static final int IDREDE = 439;

  @Override
  protected String getHomePage() {
    return HOME_PAGE;
  }

  @Override
  protected Map<String, Integer> getLojaInfo() {
    Map<String, Integer> lojaInfo = new HashMap<>();
    lojaInfo.put("IdLoja", IDLOJA);
    lojaInfo.put("IdRede", IDREDE);
    return lojaInfo;
  }

  @Override
  protected String getLoadPayload() {
    return LOAD_PAYLOAD;
  }

}
