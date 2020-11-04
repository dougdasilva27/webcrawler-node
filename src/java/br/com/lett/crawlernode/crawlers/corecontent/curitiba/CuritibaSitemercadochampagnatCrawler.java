package br.com.lett.crawlernode.crawlers.corecontent.curitiba;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-11-04
 */
public class CuritibaSitemercadochampagnatCrawler extends BrasilSitemercadoCrawler {
   public CuritibaSitemercadochampagnatCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/condor/curitiba-loja-champagnat-bigorrilho-r-martin-afonso";

   public static final int IDLOJA = 5246;
   public static final int IDREDE = 2553;

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