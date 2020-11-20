package br.com.lett.crawlernode.crawlers.corecontent.cosmopolis;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-11-04
 */
public class CosmopolisSitemercadosaovicenteCrawler extends BrasilSitemercadoCrawler {
   public CosmopolisSitemercadosaovicenteCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadossaovicente/cosmopolis-loja-sao-vicente-cosmopolis-vila-nova-r-william-neumann";

   public static final int IDLOJA = 5824;
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