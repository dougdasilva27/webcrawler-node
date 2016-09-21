package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.core.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloPanvelCrawler extends Crawler {

	private final String HOME_PAGE_HTTP = "http://www.panvel.com/";
	private final String HOME_PAGE_HTTPS = "https://www.panvel.com/";

	public SaopauloPanvelCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE_HTTP) || href.startsWith(HOME_PAGE_HTTPS));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// ID interno
			String internalId = null;
			Element elementInternalId = doc.select(".cod-produto").first();
			if (elementInternalId != null) {
				internalId = elementInternalId.text().split(":")[1].trim();
			}

			// Pid
			String internalPid = internalId;

			// Nome
			String name = null;
			Element elementName = doc.select(".det_col02 h1").first();
			if (elementName != null) {
				name = elementName.text().trim();
			}

			// Disponibilidade
			Element elementAvailable = doc.select("#itemAvisemeId").first();
			boolean available = (elementAvailable == null);

			// Preço
			Float price = null;
			if (available) {
				Element elementPrice = doc.select(".etiqueta_preco .precofinal").first();
				if (elementPrice != null) {
					price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				}
			}

			// Categorias
			Elements elementCategories = doc.select(".breadcrumb a"); 
			String category1 = null; 
			String category2 = null; 
			String category3 = null;

			String[] cat = new String[5];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			cat[3] = "";
			cat[4] = "";

			int j = 0;
			for(int i = 0; i < elementCategories.size(); i++) {
				Element e = elementCategories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "");
				j++;
			}
			category1 = cat[1];
			category2 = cat[2];
			if(elementCategories.size() > 4) {
				category3 = cat[3];
			} else {
				category3 = null;
			}

			// Imagens
			Elements elementSecondaryImages = doc.select(".ms-lightbox-template .master-slider.ms-skin-default .ms-slide .ms-thumb");
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();
			if (elementSecondaryImages.size() > 1) {
				for (int i = 1; i < elementSecondaryImages.size(); i++) { // a primeira imagem secundária é igual a primária
					Element elementSecondaryImage = elementSecondaryImages.get(i);
					String image = null;
					if (elementSecondaryImage.attr("src").contains("/4/")) {
						image = "http:" + elementSecondaryImage.attr("src").replace("/4/", "/5/").replace("-4","-5");													
					}
					else if (elementSecondaryImage.attr("src").contains("/2/")) {
						image = "http:" + elementSecondaryImage.attr("src").replace("/2/", "/5/").replace("-2","-5");							
					}
					else if (elementSecondaryImage.attr("src").contains("produtos/default.jpg") || elementSecondaryImage.attr("src").contains("/video")) { // caso esteja sem imagem
						image = "";
					}
					if (!image.equals("")) {
						secondaryImagesArray.put(image);
					}
				}
			}

			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Imagem primária
			String primaryImage = null;
			if (elementSecondaryImages.first() != null) {
				if (elementSecondaryImages.first().attr("src").contains("/4/")) {
					primaryImage = "http:" + elementSecondaryImages.first().attr("src").replace("/4/", "/5/").replace("-4.jpg","-5.jpg");
				}
				else if (elementSecondaryImages.first().attr("src").contains("/2/")) {
					primaryImage = "http:" + elementSecondaryImages.first().attr("src").replace("/2/", "/5/").replace("-2.jpg","-5.jpg");
				}

				else if (elementSecondaryImages.first().attr("src").contains("produtos/default.jpg") || elementSecondaryImages.first().attr("src").contains("/video")) {
					primaryImage = "";
				}
			}

			// Descrição
			String description = "";
			Element elementTab1 = doc.select("#tab1").first(); 
			Element elementTab2 = doc.select("#tab2").first();
			if (elementTab1 != null) {
				description += elementTab1.html();
			}
			if (elementTab2 != null) {
				description += elementTab2.html();
			}			

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
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
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getUrl());
		}

		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (url.startsWith("http://www.panvel.com/panvel/visualizarProduto") 
				|| url.startsWith("http://www.panvel.com/panvel/produto") 
				|| url.startsWith("https://www.panvel.com/panvel/visualizarProduto") 
				|| url.startsWith("https://www.panvel.com/panvel/produto"));
	}
}
