package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilMundoverdeCrawler extends VTEXNewScraper {

   private static final String HOME_PAGE = "https://www.mundoverde.com.br/";
   private static final List<String> MAIN_SELLERS = Collections.singletonList("NATUE COMERCIO E IMPORTACAO DE COSMETICOS E PRODUTOS ALIMENTÍCIOS EM GERAL S/A");

   @Override
   protected String scrapInternalpidThirdWay(JSONObject runtimeJSON) {
      return super.scrapInternalpidThirdWay(runtimeJSON);
   }

   public BrasilMundoverdeCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return MAIN_SELLERS;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
