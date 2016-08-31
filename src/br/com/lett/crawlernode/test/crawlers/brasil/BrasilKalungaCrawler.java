package br.com.lett.crawlernode.test.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.test.Logging;
import br.com.lett.crawlernode.test.kernel.task.Crawler;
import br.com.lett.crawlernode.test.kernel.task.CrawlerSession;

public class BrasilKalungaCrawler extends Crawler {

	private final String HOME_PAGE_HTTP = "http://www.kalunga.com.br/";
	private final String HOME_PAGE_HTTPS = "https://www.kalunga.com.br/";

	private final String REGEX_Z = "/\\d+z(?!_)"; 	// identificar quando temos algo do tipo /543459z
	private final String REGEX_Z_ = "/\\d+z_\\d+";	// identificar quando temos algo do tipo /543459z_

	private final String REGEX_D = "/\\d+d(?!_)"; 	// identificar quando temos algo do tipo /253564d
	private final String REGEX_D_ = "/\\d+d_\\d+";	// identificar quando tempos algo do tipo /3244356d_

	private final String REGEX_ORIGINAL = "/\\d+(?!_)\\."; // identificar quando temos algo do tipo /4534534.


	public BrasilKalungaCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE_HTTP) || href.startsWith(HOME_PAGE_HTTPS));
	}

	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// ID interno
			String internalId = null;
			Element elementInternalId = doc.select("input#hdnCodProduto").first();
			if(elementInternalId != null) {
				internalId = elementInternalId.attr("value").trim();
			}

			// Pid
			String internalPid = null;

			// Nome
			String name = null;
			Element elementName = doc.select(".product-txt h1").first();
			if(elementName != null) {
				name = elementName.text();
			}

			// Disponibilidade
			boolean available = true;
			Element elementUnavailable = doc.select(".box-price .bt_comprar_indisponivel").first();
			if(elementUnavailable != null) {
				available = false;
			}

			// Preço
			Float price = null;
			Element elementPrice = doc.select(".container-price .por .valor span").first();
			if (elementPrice != null && available) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Categoria
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementCategories = doc.select("#breadcrumbs a");
			ArrayList<String> categories = new ArrayList<String>();
			for(int i = 1; i < elementCategories.size(); i++) {
				Element e = elementCategories.get(i);
				categories.add(e.text());
			}
			for (String c : categories) {
				if (category1.isEmpty()) {
					category1 = c.trim();
				} else if (category2.isEmpty()) {
					category2 = c.trim();
				} else if (category3.isEmpty()) {
					category3 = c.trim();
				}
			}

			// Imagens
			Elements elementImages = doc.select("#galeria img");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element image : elementImages) {
				String imageSrc = image.attr("src");

				if ( !this.containsPattern(REGEX_Z, imageSrc) && !this.containsPattern(REGEX_Z_, imageSrc)) {
					if ( !this.containsPattern(REGEX_D, imageSrc) && !this.containsPattern(REGEX_D_, imageSrc) ) {
						if ( this.containsPattern(REGEX_ORIGINAL, imageSrc)) {
							imageSrc = imageSrc.replace(".jpg", "d.jpg");
						} else {
							imageSrc = imageSrc.replace("_", "d_");
						}
					}					
				}

				if(primaryImage == null) {
					primaryImage = imageSrc;
				} else {
					secondaryImagesArray.put(imageSrc);
				}
			}
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			if(primaryImage == null) {
				Element elementPrimaryImage = doc.select(".product-image .zoom img").first();
				if(elementPrimaryImage != null) {
					primaryImage = elementPrimaryImage.attr("src");
					if( !primaryImage.contains("http") ) {
						primaryImage = "http:" + primaryImage;
					}
				}
			}

			// Descrição
			String description = "";
			Element elementDescription = doc.select("#descricaoPadrao").first();
			if(elementDescription != null) {
				description = description + elementDescription.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;
			
			Product product = new Product();
			product.setSeedId(this.session.getSeedId());
			product.setUrl(this.session.getUrl());
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}

	private boolean containsPattern(String regex, String originalString) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(originalString);
		List<String> listMatches = new ArrayList<String>();

		while (matcher.find()) {
			listMatches.add(matcher.group(0));
		}

		if (listMatches.size() > 0) return true;

		return false;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (url.contains("/prod/"));
	}
}
