package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import br.com.lett.crawlernode.crawlers.extractionutils.core.YourreviewsRatingCrawler
import br.com.lett.crawlernode.util.CrawlerUtils
import models.RatingsReviews
import models.pricing.BankSlip
import org.json.JSONObject
import org.jsoup.nodes.Document
import kotlin.properties.Delegates
import models.pricing.CreditCards
import br.com.lett.crawlernode.util.MathUtils
import models.pricing.BankSlip.BankSlipBuilder
import org.jsoup.select.Elements

class BrasilToymaniaCrawler(session: Session?) : VTEXOldScraper(session) {

	 var yourViewsKey : String by Delegates.notNull();
	 var bankDiscountHtml : Double = 0.0;
	 var bankDiscountReal : Double = 0.0;

	 init {
	    this.yourViewsKey = "96498892-f7d8-4799-9eca-5b390cd962c2";
	 }
	
   override fun getHomePage(): String {
      return "https://www.toymania.com.br/";
   }

   override fun getMainSellersNames(): List<String> {
      return listOf("Toymania");
   }

	 override fun processBeforeScrapVariations(doc: Document, productJson: JSONObject, internalPid: String) {
	   val discount = CrawlerUtils.scrapIntegerFromHtml(doc, ".x-product__discount-flags #discountHightLight [class*=\"boleto\"]", true, 0);
	    
		 if(discount > 0) {
			 this.bankDiscountHtml = discount / 100.0;
		 }
		 
		 val elements = doc.select(".x-product__payment-installment-group .js--payment-installment span:not([rv-text])");
		 
		 var foundDiscount: Boolean = false;
		 val iterator = elements.iterator();
		 
		 while(iterator.hasNext() && !foundDiscount) {
			 val text = iterator.next().text().toLowerCase();

			 if(text.contains("boleto")) {
				  val number = text.replace(Regex("[^0-9+]"), "").trim();
				 
				  if(!number.isEmpty()) {
					 this.bankDiscountReal = number.toInt() / 100.0;
					}
				 
				 foundDiscount = true;
				}
			}
	 }
	
   override fun scrapDescription(doc: Document, productJson: JSONObject): String {
      return CrawlerUtils.scrapSimpleDescription(doc, mutableListOf(".x-product__description"));
   }

   override fun scrapSpotlightPrice(doc: Document, internalId: String, principalPrice: Double, comertial: JSONObject, discountsJson: JSONObject): Double {
      return principalPrice;
	 }
	
	 override fun scrapBankSlip(spotlightPrice: Double, comertial: JSONObject, discounts: JSONObject, mustSetDiscount: Boolean): BankSlip {
		 var bs: BankSlip = super.scrapBankSlip(spotlightPrice, comertial, discounts, false);
		 
		 // We have 2 bank discounts because this site show us a discount, but if we calculate the real
		 // discount, it's different from what the site shows
		 if(this.bankDiscountHtml > 0) {
			 val bankPrice: Double = MathUtils.normalizeTwoDecimalPlaces(bs.getFinalPrice() - (bs.getFinalPrice() * this.bankDiscountReal))
	
		   bs = BankSlipBuilder.create()
            .setFinalPrice(bankPrice)
            .setOnPageDiscount(this.bankDiscountHtml)
            .build();
		 }
		 
		 return bs;
	 }
	
	 override fun scrapCreditCards(comertial: JSONObject, discounts: JSONObject, mustSetDiscount: Boolean): CreditCards {
		 return super.scrapCreditCards(comertial, discounts, false);
	 }
	
   override fun scrapRating(internalId: String, internalPid: String, doc: Document, jsonSku: JSONObject): RatingsReviews {
      val ratingReviews = RatingsReviews();
      
      val yr = YourreviewsRatingCrawler(session, cookies, logger, yourViewsKey, this.dataFetcher);
      val advancedRatingReview = yr.getTotalStarsFromEachValue(internalPid);
 
      val totalNumOfEvaluations = CrawlerUtils.extractReviwsNumberOfAdvancedRatingReview(advancedRatingReview);
      val avgRating = CrawlerUtils.extractRatingAverageFromAdvancedRatingReview(advancedRatingReview);
  
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
 
      return ratingReviews;
   }
}
