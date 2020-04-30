package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXNewScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.Installment;
import models.pricing.Installment.InstallmentBuilder;

public class BrasilKitchenaidCrawler extends VTEXNewScraper {

   private static final String HOME_PAGE = "https://www.kitchenaid.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "Kitchenaid";
   private static final double AVISTA_DISCOUNT = 0.10d;

   public BrasilKitchenaidCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
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
   protected List<String> scrapSales(Document doc, JSONObject offerJson, String internalId, String internalPid) {
      String sale = CrawlerUtils.scrapStringSimpleInfo(doc, ".product__flags .price-flag", true);
      return sale != null && !sale.isEmpty() ? Arrays.asList(sale) : new ArrayList<>();
   }

   @Override
   protected Double scrapSpotlightPrice(Document doc, String internalId, JSONObject comertial) {
      Double spotlightPrice = super.scrapSpotlightPrice(doc, internalId, comertial);

      Element spotlightElement = doc.selectFirst("p[class*=\"discount-percentage-product\"] span");
      if (spotlightElement != null) {
         spotlightPrice = MathUtils.parseDoubleWithComma(spotlightElement.ownText());
      }

      return spotlightPrice;
   }

   @Override
   protected Installment setInstallment(Integer installmentNumber, Double value, Double interests, Double totalValue, Double discount) throws MalformedPricingException {
      if (installmentNumber == 1) {
         discount = AVISTA_DISCOUNT;
         value = value - (value * AVISTA_DISCOUNT);
      }

      return InstallmentBuilder.create()
            .setInstallmentNumber(installmentNumber)
            .setInstallmentPrice(value)
            .setAmOnPageInterests(interests)
            .setFinalPrice(totalValue)
            .setOnPageDiscount(discount)
            .build();
   }

   @Override
   protected BankSlip scrapBankSlip(Double spotlightPrice, JSONObject comertial) throws MalformedPricingException {
      return BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

}
