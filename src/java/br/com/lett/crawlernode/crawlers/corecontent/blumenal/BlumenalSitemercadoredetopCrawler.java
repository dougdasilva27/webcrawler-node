package br.com.lett.crawlernode.crawlers.corecontent.blumenal;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

import java.util.HashMap;
import java.util.Map;

public class BlumenalSitemercadoredetopCrawler extends BrasilSitemercadoCrawler {
   public static final String HOME_PAGE = "https://www.sitemercado.com.br/redetop/blumenau-loja-escola-agricola-asilo-r-benjamin-constant";

   public static final int IDLOJA = 4996;
   public static final int IDREDE = 2858;

   public BlumenalSitemercadoredetopCrawler(Session session) {
      super(session);
   }

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
