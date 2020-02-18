package br.com.lett.crawlernode.crawlers.corecontent.australia;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import models.prices.Prices;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class AustraliaCatchCrawler extends Crawler {

    public AustraliaCatchCrawler(Session session) {
        super(session);
        super.config.setMustSendRatingToKinesis(true);
    }

    @Override
    public List<Product> extractInformation(Document doc) throws Exception {
        super.extractInformation(doc);
        List<Product> products = new ArrayList<>();

        if (isProductPage(doc)) {
            Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

            String internalPid = crawlInternalPid(doc);
            boolean available = crawlAvailability(doc);

            String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-container.container.grid-row h1", false);
            Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".top-info__price .product--price", "content", false, '.', session);
            List<String> categories = scrapCategories(doc);

            RatingsReviews ratingsReviews = scrapRating(doc, internalPid);
            Prices prices = crawlPrices(price, doc);
            String description = crawlDescription(doc);

            JSONArray images = scrapImages(doc);
            String primaryImage = images.optString(0);
            images.remove(0);

            Elements variation = doc.select(".attribute-dropdown__option");
            Map<String, String> skus = scrapSkuVariations(variation, internalPid);

            for (Map.Entry<String, String> sku : skus.entrySet()) {

                RatingsReviews reviewsClone = ratingsReviews.clone();
                reviewsClone.setInternalId(sku.getKey());
                reviewsClone.setDate(session.getDate());
                products.add(ProductBuilder.create()
                        .setUrl(session.getOriginalURL())
                        .setInternalId(sku.getKey())
                        .setInternalPid(internalPid)
                        .setName((name + " " + sku.getValue()).trim())
                        .setPrice(price)
                        .setPrices(prices)
                        .setAvailable(available)
                        .setCategories(categories)
                        .setPrimaryImage(primaryImage)
                        .setSecondaryImages(images.toString())
                        .setRatingReviews(reviewsClone)
                        .setDescription(description)
                        .build());
            }

        } else {
            Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
        }
        return products;
    }

    private RatingsReviews scrapRating(Document doc, String internalPid) {
        RatingsReviews ratingsReviews = new RatingsReviews();
        Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".hidden > span[itemprop=ratingValue]", null, false, '.', session);
        Integer totalReview = CrawlerUtils.scrapIntegerFromHtml(doc, ".hidden > span[itemprop=reviewCount]", false, 0);

        AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(internalPid);

        ratingsReviews.setAdvancedRatingReview(advancedRatingReview);
        ratingsReviews.setTotalRating(totalReview);
        ratingsReviews.setAverageOverallRating(avgRating);
        ratingsReviews.setTotalWrittenReviews(totalReview);
        return ratingsReviews;
    }

    private AdvancedRatingReview scrapAdvancedRatingReview(String internalPid) {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.USER_AGENT, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.100 Safari/537.36");
        String homePage = "https://www.catch.com.au/";
        Request request = Request.RequestBuilder.create()
                .setUrl(homePage + "product/" + internalPid + "/review_list_ajax?limit=10000")
                .setCookies(cookies)
                .setHeaders(headers)
                .build();

        String response = dataFetcher.get(session, request).getBody();
        Document documentRating = Jsoup.parse(response);
        Elements starsElem = documentRating.select("meta[itemprop=ratingValue]");

        AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();
        int star1 = 0, star2 = 0, star3 = 0, star4 = 0, star5 = 0;

        for (Element element : starsElem) {
            Integer star = MathUtils.parseInt(element.attr("content"));
            switch (star) {
                case 1:
                    star1++;
                    break;
                case 2:
                    star2++;
                    break;
                case 3:
                    star3++;
                    break;
                case 4:
                    star4++;
                    break;
                case 5:
                    star5++;
                    break;
            }
        }
        advancedRatingReview.setTotalStar1(star1);
        advancedRatingReview.setTotalStar2(star2);
        advancedRatingReview.setTotalStar3(star3);
        advancedRatingReview.setTotalStar4(star4);
        advancedRatingReview.setTotalStar5(star5);
        return advancedRatingReview;
    }

    private Map<String, String> scrapSkuVariations(Elements doc, String internalId) {
        Map<String, String> skus = new HashMap<>();
        skus.put(internalId, "");

        for (Element element : doc.select(".attribute-dropdown__option")) {

            Element sufixoElem = element.selectFirst(".attribute-name-label");
            if (sufixoElem != null && sufixoElem.text() != null && element.select(".attribute-description").isEmpty())
                skus.put(CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".notify-me.notify-me-btn", "data-product-sku-id"),
                        sufixoElem.text());
        }
        return skus;
    }

    private List<String> scrapCategories(Document doc) {
        List<String> images = new LinkedList<>();
        for (Element elem : doc.select(".breadcrumb > span > a")) {
            images.add(elem.ownText());
        }
        images.removeIf("Home"::equals);
        return images;
    }

    private JSONArray scrapImages(Document doc) {
        JSONArray imagesArray = new JSONArray();
        for (Element elem : doc.select(".product-thumbnails__link")) {
            imagesArray.put(elem.attr("data-large-img"));
        }
        return imagesArray;
    }

    private String crawlDescription(Document doc) {
        return CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".description-block"));
    }

    private boolean isProductPage(Element doc) {
        return !doc.select(".product-container.container.grid-row").isEmpty();
    }

    private String crawlInternalPid(Element doc) {
        return CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".padded-content.buy-box.bordered-content", "data-product-id");
    }

    private boolean crawlAvailability(Element doc) {
        return !doc.select(".btn.primary.upper.large.js-buy-now-btn").isEmpty();
    }

    private Prices crawlPrices(Float price, Element doc) {
        Prices prices = new Prices();

        prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price--strike", null, false, '.', session));
        prices.setBankTicketPrice(price);
        return prices;
    }
}
