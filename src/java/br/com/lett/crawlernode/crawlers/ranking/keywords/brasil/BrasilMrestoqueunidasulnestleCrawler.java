package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MrestoqueunidasulCrawler;

/**
 * 27/04/2021
 *
 * @author Thainá Aguiar
 * <p>
 * This class was created only to extend the MrestoqueunidasulCrawler class, for organizational reasons,
 * since for the core an abstract class was created because it needs to login depending on the supplier.
 */

public class BrasilMrestoqueunidasulnestleCrawler extends MrestoqueunidasulCrawler {

   public BrasilMrestoqueunidasulnestleCrawler(Session session) {
      super(session);
   }
}
