package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import java.util.Arrays;
import java.util.List;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXNewScraper;



public class ColombiaExitoCrawler extends VTEXNewScraper {
  private static final String HOME_PAGE = "https://www.exito.com/";
  private static final String MAIN_SELLER_NAME_LOWER = "EXITO";

  public ColombiaExitoCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getHomePage() {
    return HOME_PAGE;
  }

  @Override
  protected List<String> getMainSellersNames() {
    return Arrays.asList(MAIN_SELLER_NAME_LOWER);
  }

  @Override
  protected List<Card> getCards() {
    return Arrays.asList(Card.AMEX, Card.SHOP_CARD);
  }
}
