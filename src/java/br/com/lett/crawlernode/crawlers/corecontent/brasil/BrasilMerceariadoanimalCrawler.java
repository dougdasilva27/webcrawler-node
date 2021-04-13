package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.*;
import models.Offer.OfferBuilder;
import models.prices.Prices;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilMerceariadoanimalCrawler extends Crawler {

    private static final String HOME_PAGE = "https://www.merceariadoanimal.com.br/";
    private static final String MAIN_SELLER_NAME = "Mercearia do Animal";
    private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
            Card.AMEX.toString(), Card.DINERS.toString());

    public BrasilMerceariadoanimalCrawler(Session session) {
        super(session);

    }

    @Override
    public List<Product> extractInformation(Document doc) throws Exception {
        super.extractInformation(doc);
        List<Product> products = new ArrayList<>();

        if (isProductPage(doc)) {
            Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

            String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".VariationProductSKU", true);
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#productDetailsAddToCartForm > input[name=\"product_id\"]", "value");
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".ProductMain [itemprop=\"name\"]", true) + " - " +
                    CrawlerUtils.scrapStringSimpleInfo(doc, ".ProductMain .brand > a", true);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".Breadcrumb > ul > li[itemprop]", true);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".ProductThumbImage > a", Arrays.asList("href"), "https:", HOME_PAGE);
            String secondaryImages = scrapSecondaryImages(doc, primaryImage);
            String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(
                    "#product-tabs ul> li:not(#tab-reviews)", "#product-tabs .tab-content > div:not(#reviews)"));
            RatingsReviews ratingReviews = scrapRatingsReviews(doc);
            Offers offers = scrapOffers(doc);

            // Creating the product
            Product product = ProductBuilder.create()
                    .setUrl(session.getOriginalURL())
                    .setInternalId(internalId)
                    .setInternalPid(internalPid)
                    .setName(name)
                    .setCategory1(categories.getCategory(0))
                    .setCategory2(categories.getCategory(1))
                    .setCategory3(categories.getCategory(2))
                    .setPrimaryImage(primaryImage)
                    .setSecondaryImages(secondaryImages)
                    .setDescription(description)
                    .setRatingReviews(ratingReviews)
                    .setOffers(offers)
                    .build();

            Elements selectVariations = doc.select(".ProductOptionList .VariationSelect option:not([value=\"\"])");
            Elements radioVariations = doc.select(".ProductOptionList .Value > ul > li > label > input");

            ArrayList<Element> allVariations = selectVariations;
            allVariations.addAll(radioVariations);

            if (!allVariations.isEmpty()) {
                for (Element variation : allVariations) {
                    JSONObject json = getVariationJSON(variation.attr("value"), internalPid);

                    Product clone = product.clone();

                    if (json.has("combinationid") && !json.isNull("combinationid")) {
                        clone.setInternalId(json.get("combinationid").toString());
                    }

                    Offers variationOffers = scrapVariationOffers(json);
                    clone.setName(product.getName() + ", " + variation.text().replace("(Indisponível)", "").trim());
                    clone.setOffers(variationOffers);

                    // TODO: remove this when bug is fixed - this is an almost copy paste from ProductBuilder.build()
                    // bug happens because this code only occur at ProductBuilder.build()
                    // when you clone a product and set a new offers, the other fields that are supposed
                    // to change stays the same.
                    if (variationOffers != null) {
                        for (Offer offer : variationOffers.getOffersList()) {
                            if (offer.getPricing() != null) {
                                if (offer.getIsMainRetailer()) {
                                    Pricing pricing = offer.getPricing();
                                    clone.setAvailable(true);
                                    clone.setPrices(new Prices(pricing));
                                    clone.setPrice(pricing.getSpotlightPrice().floatValue());
                                } else {
                                    Marketplace variationMkp = clone.getMarketplace();

                                    if (variationMkp == null) {
                                        variationMkp = new Marketplace();
                                    }

                                    variationMkp.add(new Seller(offer));
                                    clone.setMarketplace(variationMkp);
                                }
                            }
                        }
                    }
                    // ----------- REMOVE ABOVE -----------

                    if (json.has("instock") && json.get("instock") instanceof Boolean) {
                        clone.setAvailable(json.getBoolean("instock"));
                    }

                    products.add(clone);
                }
            } else {
                products.add(product);
            }
        } else {
            Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
        }
        return products;
    }

    private Offers scrapVariationOffers(JSONObject json) throws OfferException, MalformedPricingException {
        Offers offers = new Offers();
        Pricing pricing = scrapVariationPricing(json);

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

    private Pricing scrapVariationPricing(JSONObject json) throws MalformedPricingException {
        Double spotlightPrice = CrawlerUtils.getDoubleValueFromJSON(json, "price", false, true);

        if (spotlightPrice != null) {
            Double bankSlipPrice = null;
            Double bankSlipDiscount = null;
            CreditCards creditCards = scrapVariationCreditCards(json, spotlightPrice);

            if (json.has("checkdescmsg") && json.get("checkdescmsg") instanceof String) {
                Document slipDoc = Jsoup.parse(json.getString("checkdescmsg"));
                bankSlipPrice = CrawlerUtils.scrapDoublePriceFromHtml(slipDoc, "strong", null, true, ',', session);
                bankSlipDiscount = CrawlerUtils.scrapDoublePriceFromHtml(slipDoc, "span", null, true, ',', session) / 100;
            }

            return PricingBuilder.create()
                    .setSpotlightPrice(spotlightPrice)
                    .setCreditCards(creditCards)
                    .setBankSlip(BankSlipBuilder.create().setFinalPrice(bankSlipPrice).setOnPageDiscount(bankSlipDiscount).build())
                    .build();
        }

        return null;
    }

    private CreditCards scrapVariationCreditCards(JSONObject json, Double spotlightPrice) throws MalformedPricingException {
        CreditCards creditCards = new CreditCards();

        Installments installments = new Installments();
        installments.add(InstallmentBuilder.create()
                .setInstallmentNumber(1)
                .setInstallmentPrice(spotlightPrice)
                .build());

        if (json.has("parcmsg") && json.get("parcmsg") instanceof String) {
            String installmentText = json.getString("parcmsg");

            Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallmentFromString(installmentText, "x", "juros", true);
            if (!pair.isAnyValueNull()) {
                installments.add(InstallmentBuilder.create()
                        .setInstallmentNumber(pair.getFirst())
                        .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
                        .build());
            }
        }

        for (String brand : cards) {
            creditCards.add(CreditCardBuilder.create()
                    .setBrand(brand)
                    .setIsShopCard(false)
                    .setInstallments(installments)
                    .build());
        }

        return creditCards;
    }

    private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
        Offers offers = new Offers();
        Pricing pricing = scrapPricing(doc);

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

    private Pricing scrapPricing(Document doc) throws MalformedPricingException {
        Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".PriceRow [itemprop=\"price\"]", "content", false, '.', session);

        if (spotlightPrice != null) {
            Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product .promotion", null, true, ',', session);
            Double bankSlipPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".MsgBoleto strong", null, true, ',', session);
            Double bankSlipDiscount = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".MsgBoleto span", null, true, ',', session) / 100;
            CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

            return PricingBuilder.create()
                    .setSpotlightPrice(spotlightPrice)
                    .setPriceFrom(priceFrom)
                    .setCreditCards(creditCards)
                    .setBankSlip(BankSlipBuilder.create().setFinalPrice(bankSlipPrice).setOnPageDiscount(bankSlipDiscount).build())
                    .build();
        }

        return null;
    }

    private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
        CreditCards creditCards = new CreditCards();

        Installments installments = new Installments();
        installments.add(InstallmentBuilder.create()
                .setInstallmentNumber(1)
                .setInstallmentPrice(spotlightPrice)
                .build());

        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".MsgParcelamento", doc, true, "x de", "juros", true, ',');
        if (!pair.isAnyValueNull()) {
            installments.add(InstallmentBuilder.create()
                    .setInstallmentNumber(pair.getFirst())
                    .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
                    .build());
        }

        for (String brand : cards) {
            creditCards.add(CreditCardBuilder.create()
                    .setBrand(brand)
                    .setIsShopCard(false)
                    .setInstallments(installments)
                    .build());
        }

        return creditCards;
    }

    private boolean isProductPage(Document doc) {
        return doc.selectFirst("#product-main") != null;
    }

    private String scrapSecondaryImages(Document doc, String primaryImage) {
        String secondaryImages = null;
        JSONArray secondaryImagesArray = new JSONArray();
        Elements imagesElement = doc.select(".ProductTinyImageList > ul > li > div > div > a");

        for (Element imageElement : imagesElement) {
            if (imageElement.hasAttr("rel")) {
                JSONObject json = JSONUtils.stringToJson(imageElement.attr("rel"));

                if (json.has("largeimage") && json.get("largeimage") instanceof String) {

                    String image = json.getString("largeimage");

                    if (!image.equals(primaryImage)) {
                        secondaryImagesArray.put(image);
                    }
                }
            }
        }

        if (secondaryImagesArray.length() > 0) {
            secondaryImages = secondaryImagesArray.toString();
        }

        return secondaryImages;
    }

    private RatingsReviews scrapRatingsReviews(Document doc) {
        RatingsReviews ratingReviews = new RatingsReviews();
        ratingReviews.setDate(session.getDate());

        Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtmlAttr(doc,
                "[itemprop=\"aggregateRating\"] meta[itemprop=\"ratingCount\"]", "content", 0);
        Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, "[itemprop=\"aggregateRating\"] meta[itemprop=\"ratingValue\"]", "content", false, '.', session);
        Integer totalWrittenReviews = doc.select(".ProductReviewList > li").size();

        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating != null ? avgRating : 0.0d);
        ratingReviews.setTotalWrittenReviews(totalWrittenReviews);

        return ratingReviews;
    }

    private JSONObject getVariationJSON(String variation, String internalPid) {
        JSONObject variationJSON = new JSONObject();

        Request request = RequestBuilder.create()
                .setUrl("https://www.merceariadoanimal.com.br/ajax.ecm?w=GetVariationOptions&productId=" + internalPid + "&options=" + variation)
                .build();

        variationJSON = JSONUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

        return variationJSON;
    }

    private Prices scrapVariationPrices(Prices prices, JSONObject json, Float price) {
        if (price == null) {
            return new Prices();
        }

        Prices clone = prices.clone();

        if (json.has("checkdescmsg") && json.get("checkdescmsg") instanceof String) {
            clone.setBankTicketPrice(CrawlerUtils.scrapFloatPriceFromString(json.getString("checkdescmsg"), ',', "(", ")", session));
        }

        Map<Integer, Float> installmentPriceMap = new TreeMap<>();
        installmentPriceMap.put(1, price);

        if (json.has("parcmsg") && json.get("parcmsg") instanceof String) {
            String installmentText = json.getString("parcmsg");

            Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallmentFromString(installmentText, "x", "juros", true);
            if (!installment.isAnyValueNull()) {
                installmentPriceMap.put(installment.getFirst(), installment.getSecond());
            }

            clone.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
            clone.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
            clone.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
            clone.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
        }

        return clone;
    }
}
