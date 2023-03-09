package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.LifeappsCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import exceptions.MalformedPricingException;
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Date: 31/07/2019
 *
 * @author Joao Pedro
 */
public class BrasilJcdistribuicaoCrawler extends LifeappsCrawler {

   private static final String HOME_PAGE = "https://jcdistribuicao.superon.app/";
   private static final String COMPANY_ID = "6f0ae38d-50cd-4873-89a5-6861467b5f52";    //never changes
   private String API_HASH(){return this.session.getOptions().optString("api_hash");} //can change, but is working since 2023
   private static final String FORMA_PAGAMENTO = "3dedac19-6643-4401-9c08-aac81d6edb7c";  //never changes
   private static final String SELLER_NAME_LOWER = "jc distribuicao brasil";

   public BrasilJcdistribuicaoCrawler(Session session) {
      super(session);
   }

   @Override
   public String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   public String getCompanyId() {
      return COMPANY_ID;
   }

   @Override
   public String getApiHash() {
      return API_HASH();
   }

   @Override
   public String getFormaDePagamento() {
      return FORMA_PAGAMENTO;
   }

   @Override
   public String getSellerName() {
      return SELLER_NAME_LOWER;
   }

   @Override
   protected Pricing scrapPricing(JSONObject product) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(product, "preco_sem_politica_varejo", false);

      BigDecimal bdSpotlightPrice = BigDecimal.valueOf(spotlightPrice);
      spotlightPrice = bdSpotlightPrice.setScale(2, RoundingMode.DOWN).doubleValue();

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = super.scrapCreditcards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(null)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();

   }
}
