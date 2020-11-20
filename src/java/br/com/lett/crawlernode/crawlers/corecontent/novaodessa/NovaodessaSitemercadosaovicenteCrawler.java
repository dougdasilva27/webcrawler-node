package br.com.lett.crawlernode.crawlers.corecontent.novaodessa;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-11-04
 */
public class NovaodessaSitemercadosaovicenteCrawler extends BrasilSitemercadoCrawler {
   public NovaodessaSitemercadosaovicenteCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadossaovicente/nova-odessa-loja-sao-vicente-ampelio-gazzetta-parque-industrial-harmonia-av-ampelio-gazzetta";

   public static final int IDLOJA = 4664;
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