package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilErgovitaCrawler extends Crawler {

    private static final String HOME_PAGE = "https://www.ergovita.com.br/";
    private static final String MAIN_SELLER_NAME = "Ergovita";
    protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
            Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

    public BrasilErgovitaCrawler(Session session) {
        super(session);
    }

    @Override
    public List<Product> extractInformation(Document doc) throws Exception {
        List<Product> products = new ArrayList<>();

        if (isProductPage(doc)) {
            JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", "", null, false, false);

            String internalId = jsonInfo.optString("productID");
            String internalPid = internalId;
            String name = jsonInfo.optString("name");

            //TODO get right image
            String primaryImage = scrapPrimaryImage(jsonInfo);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".EstPathCatLink");
            String description = CrawlerUtils.scrapElementsDescription(doc, Collections.singletonList("#det-product-description-tab > div:not(:last-child)"));

            //TODO get rating from the json
            RatingsReviews ratingReview = scrapRatingReviews(jsonInfo);
            Offers offers = scrapOffers(doc, jsonInfo);

            Product product = ProductBuilder.create()
                    .setUrl(session.getOriginalURL())
                    .setInternalId(internalId)
                    .setInternalPid(internalPid)
                    .setName(name)
                    .setCategory1(categories.getCategory(0))
                    .setCategory2(categories.getCategory(1))
                    .setCategory3(categories.getCategory(2))
                    .setPrimaryImage(primaryImage)
                    .setDescription(description)
                    .setRatingReviews(ratingReview)
                    .setOffers(offers)
                    .build();

            products.add(product);
        } else {
            Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
        }

        return products;
    }

    private boolean isProductPage(Document doc) {
        return doc.selectFirst(".det-product-container") != null;
    }

    private String scrapPrimaryImage(JSONObject json) {
        String imgUrl = json.optString("image");
        return null;
    }

    private Offers scrapOffers(Document doc, JSONObject jsonInfo) throws OfferException, MalformedPricingException {
        //TODO corrigir para produtos indisponiveis
        Offers offers = new Offers();
        Pricing pricing = scrapPricing(doc, jsonInfo);

        if (pricing != null) {
            offers.add(OfferBuilder.create()
                    .setUseSlugNameAsInternalSellerId(true)
                    .setSellerFullName(MAIN_SELLER_NAME)
                    .setSellersPagePosition(1)
                    .setIsBuybox(false)
                    .setIsMainRetailer(true)
                    .setPricing(pricing)
                    .build());
        }

        return offers;
    }

    private Pricing scrapPricing(Document doc, JSONObject jsonInfo) throws MalformedPricingException {
        Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "b .FCPriceValue", null, false, ',', this.session);

        if (spotlightPrice != null) {
            Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "strike .FCPriceValue", null, false, ',', this.session);
            CreditCards creditCards = scrapCreditCards(spotlightPrice);
            BankSlip bankSlip = scrapBankSlip(jsonInfo);

            return PricingBuilder.create()
                    .setSpotlightPrice(spotlightPrice)
                    .setPriceFrom(priceFrom)
                    .setCreditCards(creditCards)
                    .setBankSlip(bankSlip)
                    .build();
        }

        return null;
    }

    private BankSlip scrapBankSlip(JSONObject jsonInfo) throws MalformedPricingException {
        BankSlip bankSlip = new BankSlip();
        JSONObject bankSlipOffer = jsonInfo.optJSONObject("offers");
        if (bankSlipOffer != null && !bankSlipOffer.isEmpty()) {
            Double bankSlipPrice = bankSlipOffer.optDouble("price");
            bankSlip = BankSlip.BankSlipBuilder.create()
                    .setFinalPrice(bankSlipPrice)
                    .build();
        }
        return bankSlip;
    }

    private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
        CreditCards creditCards = new CreditCards();

        Installments installments = new Installments();
        installments.add(InstallmentBuilder.create()
                .setInstallmentNumber(1)
                .setInstallmentPrice(spotlightPrice)
                .build());

        for (String brand : cards) {
            creditCards.add(CreditCardBuilder.create()
                    .setBrand(brand)
                    .setIsShopCard(false)
                    .setInstallments(installments)
                    .build());
        }

        return creditCards;
    }

    private RatingsReviews scrapRatingReviews(JSONObject json) {
        RatingsReviews ratingReviews = new RatingsReviews();
        ratingReviews.setDate(session.getDate());

        int totalComments = 0;
        double avgRating = 0D;
        AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();

        JSONArray reviews = json.optJSONArray("review");
        JSONObject aggregateRating = json.optJSONObject("aggregateRating");
        if (reviews != null && aggregateRating != null) {
            totalComments = aggregateRating.optInt("reviewCount", 0);
            avgRating = aggregateRating.optDouble("ratingValue", 0);
            advancedRatingReview = scrapAdvancedRatingReview(reviews);
        }

        ratingReviews.setTotalRating(totalComments);
        ratingReviews.setTotalWrittenReviews(totalComments);
        ratingReviews.setAverageOverallRating(avgRating);
        ratingReviews.setAdvancedRatingReview(advancedRatingReview);
        return ratingReviews;
    }

    private AdvancedRatingReview scrapAdvancedRatingReview(JSONArray reviews) {
        int star1 = 0;
        int star2 = 0;
        int star3 = 0;
        int star4 = 0;
        int star5 = 0;

        for (Object obj : reviews) {
            JSONObject review = (JSONObject) obj;
            JSONObject reviewValue = review.optJSONObject("reviewRating");

            switch (reviewValue.optInt("ratingValue")) {
                case 5:
                    star5 += 1;
                    break;
                case 4:
                    star4 += 1;
                    break;
                case 3:
                    star3 += 1;
                    break;
                case 2:
                    star2 += 1;
                    break;
                case 1:
                    star1 += 1;
                    break;
                default:
                    break;
            }
        }

        return new AdvancedRatingReview.Builder()
                .totalStar1(star1)
                .totalStar2(star2)
                .totalStar3(star3)
                .totalStar4(star4)
                .totalStar5(star5)
                .build();
    }
}
