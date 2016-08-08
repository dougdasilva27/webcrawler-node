package br.com.lett.crawlernode.crawlers.ribeiraopreto;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.models.Product;
import br.com.lett.crawlernode.util.Logging;

public class RibeiraopretoSavegnagoCrawler extends Crawler {

	/*
	 * 	Ribeirão Preto - 1
	 *	Sertãozinho - 6
	 *	Jardinópolis - 11
	 *	Jaboticabal - 7
	 *	Franca - 3
	 *	Barretos - 10
	 *	Bebedouro - 9
	 *	Monte Alto - 12
	 *	Araraquara - 4
	 *	São carlos - 5
	 *	Matão - 8
	 */
	
	private final String HOME_PAGE = "http://www.savegnagoonline.com.br/";
	
	public RibeiraopretoSavegnagoCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public String handleURLBeforeFetch(String curURL) {

		if(curURL.endsWith("/p")) {
			try {
				String url = curURL;
				List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");
				List<NameValuePair> paramsNew = new ArrayList<NameValuePair>();

				for (NameValuePair param : paramsOriginal) {
					if (!param.getName().equals("sc")) {
						paramsNew.add(param);
					}
				}

				paramsNew.add(new BasicNameValuePair("sc", "1"));
				URIBuilder builder = new URIBuilder(curURL.toString().split("\\?")[0]);

				builder.clearParameters();
				builder.setParameters(paramsNew);

				return builder.build().toString();

			} catch (URISyntaxException e) {
				return curURL;
			}
		}

		return curURL;

	}

	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, "Product page identified: " + this.session.getUrl());

			// ID interno
			String internalId = null;
			Element elementInternalId = doc.select(".productReference").first();
			if (elementInternalId != null) {
				internalId = elementInternalId.text().trim();
			}

			// Pid
			String internalPid = internalId;

			// Nome
			String name = null;
			Element elementName = doc.select(".fn.productName").first();
			if (elementName != null) {
				name = elementName.text().trim();
			}

			// Price
			Float price = null;
			Element elementPrice = doc.select(".skuBestPrice").first();
			if(elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Disponibilidade
			boolean available = true;
			if(price == null) {
				available = false;
			}

			// Categorias
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementCategories = doc.select(".col-xs-25 .bread-crumb li a");

			if (elementCategories.size() > 1) {
				for(int i = 1; i < elementCategories.size(); i++) {
					if (category1.isEmpty()) {
						category1 = elementCategories.get(i).text();
					} 
					else if (category2.isEmpty()) {
						category2 = elementCategories.get(i).text();
					} 
					else if (category3.isEmpty()) {
						category3 = elementCategories.get(i).text();
					}
				}
			}

			// Imagens
			String primaryImage = "";
			String secondaryImages = "";
			JSONArray secondaryImagesArray = new JSONArray();
			Element elementPrimaryImage = doc.select("#image-main").first();

			if (elementPrimaryImage != null) {
				primaryImage = elementPrimaryImage.attr("src").trim();
			}
			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescription = doc.select(".productDescriptionWrap").first();
			Element elementSpecification = doc.select(".productSpecificationWrap").first();
			if (elementDescription != null) {
				description = description + elementDescription.html();
			}
			if (elementSpecification != null) {
				description = description + elementSpecification.html(); 
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setSeedId(session.getSeedId());
			product.setUrl(session.getUrl());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);
			product.setAvailable(available);

			products.add(product);

		} else {
			Logging.printLogTrace(logger, "Not a product page" + session.getSeedId());
		}
		
		return products;
	}
	
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (url.startsWith("http://www.savegnagoonline.com.br/") && url.contains("/p?sc="));
	}

}
