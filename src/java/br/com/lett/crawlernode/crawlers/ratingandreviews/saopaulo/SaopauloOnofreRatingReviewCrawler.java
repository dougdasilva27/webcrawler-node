package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebElement;

import br.com.lett.crawlernode.core.crawler.RatingReviewCrawler;
import br.com.lett.crawlernode.core.fetcher.Fetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class SaopauloOnofreRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloOnofreRatingReviewCrawler(Session session) {
		super(session);
		this.config.setFetcher(Fetcher.WEBDRIVER);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(document)) {
			
			RatingsReviews ratingReviews = crawlRating(document);
			ratingReviews.setInternalId(crawlInternalId(document));

			ratingReviewsCollection.addRatingReviews(ratingReviews);
		}


		return ratingReviewsCollection;
	}

	private RatingsReviews crawlRating(Document document) {
		RatingsReviews ratingReviews = new RatingsReviews();

		ratingReviews.setDate(session.getDate());

		// get a list of page numbers
		List<String> pageNumbers = new ArrayList<>();
		Elements pageNumberElements = document.select("#cphConteudo_dpComents [class^=numericButton]");
		for (Element pageNumberElement : pageNumberElements) {
			pageNumbers.add(pageNumberElement.text().trim());
		}

		// iterate through each page and click on the ones that are not already selected
		Integer totalEvaluations = 0;
		Double totalRating = 0.0;
		for (String pageNumber : pageNumbers) {
			Document currentDocument;
			WebElement pageElement = selectPageElement(pageNumber);
			boolean isSelected = "numericButton_current".equals(pageElement.getAttribute("class"));

			if (isSelected) {
				currentDocument = document;
			} else {					
				this.webdriver.clickOnElementViaJavascript(pageElement);
				String html = this.webdriver.findElementByCssSelector("html").getAttribute("innerHTML");
				currentDocument = Jsoup.parse(html);
			}

			Elements ratingElements = currentDocument.select("#cphConteudo_upComentarios .comment span.rating-flat");
			for (Element ratingElement : ratingElements) {
				totalEvaluations++;
				String ratingText = ratingElement.attr("class");
				List<String> parsedNumbers = MathCommonsMethods.parseNumbers(ratingText);
				if (!parsedNumbers.isEmpty()) {
					if ("10".equals(parsedNumbers.get(0))) totalRating += 1;
					if ("20".equals(parsedNumbers.get(0))) totalRating += 2;
					if ("30".equals(parsedNumbers.get(0))) totalRating += 3;
					if ("40".equals(parsedNumbers.get(0))) totalRating += 4;
					if ("50".equals(parsedNumbers.get(0))) totalRating += 5;
				}
			}
		}

		if (totalEvaluations > 0) {
			Double avgRating = MathCommonsMethods.normalizeTwoDecimalPlaces(totalRating / totalEvaluations);

			ratingReviews.setTotalReviews(totalEvaluations);
			ratingReviews.setAverageOverallRating(avgRating);
		}

		return ratingReviews;
	}

	private WebElement selectPageElement(String pageNumber) {
		List<WebElement> ratingPagesElements = this.webdriver.findElementsByCssSelector("#cphConteudo_dpComents [class^=numericButton]");
		for (WebElement page : ratingPagesElements) {
			if (pageNumber.equals(page.getText())) {
				return page;
			}
		}
		return null;
	}

	private boolean isProductPage(Document document) {
		Elements skuList = document.select(".sku-radio .sku-list li");
		return skuList.size() > 0;
	}

	private String crawlInternalId(Document document) {
		String internalId = null;

		Elements elementInternalID = document.select(".main-product-info .main-product-name input[type=hidden]");
		if(elementInternalID.size() > 0) {
			internalId = elementInternalID.first().attr("value");
		}

		return internalId;
	}

}
