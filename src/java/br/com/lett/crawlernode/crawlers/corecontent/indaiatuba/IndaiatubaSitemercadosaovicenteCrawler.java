package br.com.lett.crawlernode.crawlers.corecontent.indaiatuba;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-11-04
 */
public class IndaiatubaSitemercadosaovicenteCrawler extends BrasilSitemercadoCrawler {
   public IndaiatubaSitemercadosaovicenteCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadossaovicente/indaiatuba-loja-sao-vicente-kioto-jardim-kioto-i-av-francisco-de-paula-leite";

   public static final int IDLOJA = 5826;
   public static final int IDREDE = 2663;

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
}