package br.com.lett.crawlernode.crawlers.corecontent.hortolandia;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-11-04
 */
public class HortolandiaSitemercadosaovicenteCrawler extends BrasilSitemercadoCrawler {
   public HortolandiaSitemercadosaovicenteCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadossaovicente/hortolandia-loja-sao-vicente-hortolandia-jardim-das-paineiras-r-orestes-denadai";

   public static final int IDLOJA = 5821;
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