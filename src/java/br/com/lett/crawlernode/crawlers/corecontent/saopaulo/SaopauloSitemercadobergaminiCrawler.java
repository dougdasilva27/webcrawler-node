package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author gabriel date: 2019-09-24
 */
public class SaopauloSitemercadobergaminiCrawler extends BrasilSitemercadoCrawler {

  public SaopauloSitemercadobergaminiCrawler(Session session) {
    super(session);
  }

  public static final String HOME_PAGE =
          "https://www.sitemercado.com.br/bergamini/sao-paulo-hiper-vila-constanca-avenida-luis-stamatis/";
  public static final String LOAD_PAYLOAD =
          "{\"lojaUrl\":\"sao-paulo-hiper-vila-constanca-avenida-luis-stamatis\",\"redeUrl\":\"bergamini\"}";

  public static final int IDLOJA = 471;
  public static final int IDREDE = 414;

  @Override
  protected String getHomePage() {
    return HOME_PAGE;
  }

  @Override
  protected String getLoadPayload() {
    return LOAD_PAYLOAD;
  }

  @Override
  protected Map<String, Integer> getLojaInfo() {
    Map<String, Integer> lojaInfo = new HashMap<>();
    lojaInfo.put("IdLoja", IDLOJA);
    lojaInfo.put("IdRede", IDREDE);
    return lojaInfo;
  }
}
