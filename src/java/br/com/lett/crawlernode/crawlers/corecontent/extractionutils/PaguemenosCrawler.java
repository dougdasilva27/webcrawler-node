package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.Arrays;
import java.util.List;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.session.Session;

public class PaguemenosCrawler extends VTEXNewScraper {

  public PaguemenosCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.paguemenos.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "pague menos";
  private static final String MAIN_SELLER_NAME_LOWER_2 = "farmácias pague menos";

  @Override
  protected String getHomePage() {
    return HOME_PAGE;
  }

  @Override
  protected List<String> getMainSellersNames() {
    return Arrays.asList(MAIN_SELLER_NAME_LOWER, MAIN_SELLER_NAME_LOWER_2);
  }

  @Override
  protected List<Card> getCards() {
    return Arrays.asList(Card.VISA, Card.SHOP_CARD);
  }

}
