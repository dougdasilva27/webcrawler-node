package br.com.lett.crawlernode.crawlers.corecontent.teresina;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class TeresinaSitemercadoferreiraportoalegreCrawler extends BrasilSitemercadoCrawler {

   public TeresinaSitemercadoferreiraportoalegreCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE =
         "https://www.sitemercado.com.br/ferreira/teresina-loja-porto-alegre-esplanada-avenida-ayrton-sena";
   public static final String LOAD_PAYLOAD =
         "{\"lojaUrl\":\"teresina-loja-porto-alegre-esplanada-avenida-ayrton-sena\",\"redeUrl\":\"ferreira\"}";

   public static final int IDLOJA = 1297;
   public static final int IDREDE = 708;

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
