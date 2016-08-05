package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.models.Product;
import br.com.lett.crawlernode.util.Logging;

public class BrasilMagazineluizaCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.magazineluiza.com.br/";

	public BrasilMagazineluizaCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			/*
			 * Id interno -- obtido a partir do id do sku apendado com o full id. O full id será
			 * apendado no início do tratamento de cada caso de produto (produto com variação 
			 * e sem variação) 
			 */
			Element elementInternalId = doc.select("small[itemprop=productID]").first();
			int begin = elementInternalId.text().indexOf(".com") + 4;
			String internalId = elementInternalId.text().substring(begin).replace(")", "").trim();

			// Pid
			String internalPid = internalId;

			// Nome
			String name = null;
			Element elementName = doc.select("h1[itemprop=name]").first();
			if (elementName != null) {
				name = elementName.text();
			}

			// Categorias
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementsCategories = doc.select(".container-bread-crumb-detail.bread-none-line ul li[typeof=v:Breadcrumb] a");
			ArrayList<String> categories = new ArrayList<String>();
			for(Element categorie : elementsCategories) {
				String cat = categorie.text();
				if (!cat.equals("magazineluiza.com")) {
					categories.add(cat);
				}
			}
			for (String category : categories) {
				if (category1.isEmpty()) {
					category1 = category;
				} else if (category2.isEmpty()) {
					category2 = category;
				} else if (category3.isEmpty()) {
					category3 = category;
				}
			}

			// Imagens
			String primaryImage = null;
			String secondaryImages = null;
			Elements elementsImages = doc.select(".container-little-picture ul li a");
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element e : elementsImages) {
				if( !e.attr("rel").isEmpty() ) {
					String image = parseImage(e.attr("rel"));
					if (primaryImage == null) {
						primaryImage = image;
					} else {
						secondaryImagesArray.put(image);
					}
				}
			}
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescription = doc.select(".factsheet-main-container").first();
			if (elementDescription != null) {
				description = description + elementDescription.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Elements variationSelector = doc.select(".js-buy-option-box.container-basic-information .js-buy-option-list");

			// Apenas um produto
			if (variationSelector.size() == 0) {

				// Disponibilidade
				boolean available = true;
				Element elementAvailable = doc.select(".container-btn-buy").first();
				if(elementAvailable == null) {
					available = false;
				} else {
					Elements elementsMarketPlace = doc.select(".market-place-delivery .market-place-delivery__seller--big");
					boolean magazineIsSelling = false;
					for(Element e : elementsMarketPlace) {
						if(e.text().equals("Magazine Luiza")) {
							magazineIsSelling = true;
							break;
						}
					}
					if(magazineIsSelling) available = true;
					else available = false;
				}

				// Preco
				Float price = null;
				if (available) {
					Element elementPrice = doc.select(".content-buy-product meta[itemprop=price]").first();
					if(elementPrice != null) {
						price = Float.parseFloat(elementPrice.attr("content").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
					}
				}

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
			}

			// Múltiplas variacões
			else {

				// Identificar se tem o seletor de cor (caso a parte)
				Elements colorsSelector = doc.select(".pd-color-select-box");

				if(colorsSelector.size() > 0) { // seletor de cor

					variationSelector = variationSelector.first().select("li");

					for(Element variation : variationSelector) {

						/*
						 * No caso de seletor de cores, existe uma url para cada cor
						 */

						String fullId = null;
						String nameSecondPart = null;

						// Id interno
						fullId = variation.select("input").first().attr("value");
						String variationInternalId = internalId + "-" + fullId;

						// Pid
						String variationInternalPid = internalId;

						// Nome
						nameSecondPart = variation.select("span img").first().attr("title");
						String variationName = name + " - " + nameSecondPart;

						// Construir a URL do produto
						String variationUrl = assembleVariationUrl(this.session.getUrl(), variationInternalId);
						Document variationDocument =  DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, variationUrl, null, null);

						// Disponibilidade
						boolean available = true;
						Element elementAvailale = variationDocument.select(".container-btn-buy").first();
						if(elementAvailale == null) {
							available = false;
						}

						// Preco
						Float price = null;
						if (available) {
							Element elementPrice = variationDocument.select(".content-buy-product meta[itemprop=price]").first();
							if(elementPrice != null) {
								price = Float.parseFloat(elementPrice.attr("content").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
							}
						}

						// Imagens
						String variationPrimaryImage = null;
						String variationSecondaryImages = null;
						Elements variationElementsImages = variationDocument.select(".container-little-picture ul li a");
						JSONArray variationSecondaryImagesArray = new JSONArray();

						for(Element e : variationElementsImages) {
							if( !e.attr("rel").isEmpty() ) {
								String variationImage = parseImage(e.attr("rel"));
								if (variationPrimaryImage == null) {
									variationPrimaryImage = variationImage;
								} else {
									variationSecondaryImagesArray.put(variationImage);
								}
							}
						}
						if (variationSecondaryImagesArray.length() > 0) {
							variationSecondaryImages = variationSecondaryImagesArray.toString();
						}



						Product product = new Product();
						product.setSeedId(this.session.getSeedId());
						product.setUrl(this.session.getUrl());
						product.setInternalId(variationInternalId);
						product.setInternalPid(variationInternalPid);
						product.setName(variationName);
						product.setPrice(price);
						product.setCategory1(category1);
						product.setCategory2(category2);
						product.setCategory3(category3);
						product.setPrimaryImage(variationPrimaryImage);
						product.setSecondaryImages(variationSecondaryImages);
						product.setDescription(description);
						product.setStock(stock);
						product.setMarketplace(marketplace);
						product.setAvailable(available);

						products.add(product);

					}
				}

				else { // sem seletor de cor

					variationSelector = variationSelector.select("li");

					for(Element variation : variationSelector) {

						String fullId = null;
						String nameSecondPart = null;					

						fullId = variation.select("input").first().attr("value");
						nameSecondPart = variation.select("span").first().text();

						// Id interno
						String variationInternalId = internalId + "-" + fullId;

						// Pid
						String variationInternalPid = internalId;

						// Nome
						String variationName = name + " - " + nameSecondPart;

						// Disponibilidade
						boolean available = true;
						Element elementAvailale = doc.select(".container-btn-buy").first();
						if(elementAvailale == null) {
							available = false;
						}

						// Preco
						Float price = null;
						if (available) {
							Element elementPrice = doc.select(".content-buy-product meta[itemprop=price]").first();
							if(elementPrice != null) {
								price = Float.parseFloat(elementPrice.attr("content").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
							}
						}

						Product product = new Product();
						product.setSeedId(this.session.getSeedId());
						product.setUrl(this.session.getUrl());
						product.setInternalId(variationInternalId);
						product.setInternalPid(variationInternalPid);
						product.setName(variationName);
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

					}
				}
			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}

	private String parseImage(String text) {
		int begin = text.indexOf("largeimage:") + 11;
		String img = text.substring(begin);
		img = img.replace("\'", " ").replace('}', ' ').trim();

		return img;
	}

	private String assembleVariationUrl(String originalUrl, String fullInternalId) {

		// pegar os dois últimos dígitos do fullInternalId
		int begin = (fullInternalId.length()-1) - 1;
		String digits = fullInternalId.substring(begin);
		String[] tokens = originalUrl.split("/");
		String newUrl = "http://www.magazineluiza.com.br";
		for(int i = 3; i < tokens.length; i++) {
			newUrl = newUrl + "/" + tokens[i];
			if(i == 5) newUrl = newUrl + "/" + digits;
		}


		return newUrl;

	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (url.contains("/p/"));
	}

}