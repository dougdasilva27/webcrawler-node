package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.crawlers.extractionutils.core.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SaopauloEmporioquatroestrelasCrawler extends VTEXOldScraper {

    private static final String HOME_PAGE = "https://www.emporioquatroestrelas.com.br/";
    private static final List<String> SELLERS = Collections.singletonList("Empório Quatro estrelas");

    public SaopauloEmporioquatroestrelasCrawler(Session session) {
        super(session);
    }

    @Override
    protected String getHomePage() {
        return HOME_PAGE;
    }

    @Override
    protected List<String> getMainSellersNames() {
        return SELLERS;
    }

    @Override
    protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
        YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger, "fc953c56-aa61-4a5a-a8a0-15fd32fb0e95", this.dataFetcher);

        RatingsReviews ratingReviews = new RatingsReviews();
        ratingReviews.setDate(session.getDate());

        Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, "fc953c56-aa61-4a5a-a8a0-15fd32fb0e95", dataFetcher);
        Integer totalNumOfEvaluations = yourReviews.getTotalNumOfRatingsFromYourViews(docRating);
        Double avgRating = yourReviews.getTotalAvgRatingFromYourViews(docRating);
        AdvancedRatingReview advancedRatingReview = yourReviews.getTotalStarsFromEachValue(internalPid);

        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);
        ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
        ratingReviews.setAdvancedRatingReview(advancedRatingReview);

        return ratingReviews;
    }

    @Override
    protected String scrapDescription(Document doc, JSONObject productJson) {
        StringBuilder description = new StringBuilder();

        if (productJson.optJSONArray("Benefícios do produto") != null) {
            description.append("<div>");
            description.append(productJson.optJSONArray("Benefícios do produto").get(0));
            description.append("<div>");
        }

        if (productJson.optJSONArray("Importante") != null) {
            description.append("<div>");
            description.append(productJson.optJSONArray("Importante").get(0));
            description.append("<div>");
        }

        description.append(CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".prod-dica", ".prod-tabela")));
        return description.toString();
    }
}
