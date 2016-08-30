package br.com.lett.crawlernode.crawlers.brasil;


import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class BrasilNetshoesCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.netshoes.com.br/";

	public BrasilNetshoesCrawler(CrawlerSession session) {
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

		if( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// Pré nome
			Elements elementPreName = doc.select(".product-name-holder h1.base-title");
			String preName = elementPreName.text().replace("'","").replace("’","").trim();

			// Preço
			Float price = null;
			Element elementPrice = doc.select(".price-holder .new-price").first();
			if(elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Categorias
			Elements elementCategories = doc.select(".body-holder .breadcrumbs li a"); 
			String category1 = "";
			String category2 = "";
			String category3 = "";

			for(Element e : elementCategories) {
				if(category1.isEmpty()) {
					category1 = e.text();
				}
				else if(category2.isEmpty()) {
					category2 = e.text();
				}
				else if(category3.isEmpty()) {
					category3 = e.text();
				}
			}

			// Imagens
			Elements elementImages = doc.select(".photo-gallery-list").select("a");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element e : elementImages) {

				if(primaryImage == null) {
					primaryImage = "http:" + e.attr("data-zoom");
				} 
				else {
					secondaryImagesArray.put("http:" + e.attr("data-zoom"));
				}

			}

			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";   
			Elements element_descricao = doc.select("#caracteristicas");
			description = element_descricao.first().text().replace(".", ".\n").replace("'","").replace("’","").trim();

			// Marketplace
			JSONArray marketplace = null;

			Element elementSku = doc.select("form[name=addToCart] input[name=skuId]").first();

			// Capturando sku genérico
			String sku = "";
			String[] skuSpplited = elementSku.attr("value").split("-");

			for(String s: skuSpplited) {
				sku = sku + s + "-";
			}

			// Pegar produtos dentro da url usando a variável da chaordic

			JSONObject chaordicMeta = null; //FIXME - variável da chaordic não existe mais

			Elements scriptTags = doc.getElementsByTag("script");
			for (Element tag : scriptTags){                
				for (DataNode node : tag.dataNodes()) {
					if(node.getWholeData().trim().startsWith("window.chaordic_meta = ")) {									
						chaordicMeta = new JSONObject(node.getWholeData().trim().substring(22));
					}
				}        
			}

			if(chaordicMeta != null && chaordicMeta.length() > 0) {

				JSONArray skus = chaordicMeta.getJSONObject("product").getJSONArray("skus");
				JSONArray sizes_information = chaordicMeta.getJSONObject("product").getJSONObject("specs").getJSONArray("size");

				for(int i = 0; i < skus.length(); i++) {

					JSONObject skuInformation = skus.getJSONObject(i);

					if(skuInformation.getString("url").equals(this.session.getUrl())) {

						// ID interno
						String internalID = skuInformation.getString("sku");

						// Nome
						String name = preName;

						// Se tiver múltiplos produtos, vamos mudar os nomes de acordo com o seu tamanho
						if(skus.length() > 1) {

							Elements elementsSizes = doc.select("form[name=addToCart] ul.attr-size .attr-name");

							// Tem múltiplos tamanhos
							if(elementsSizes.size() > 0) {

								String sizeLabel = "";

								for(int x = 0; x < sizes_information.length(); x++) {
									if(sizes_information.getJSONObject(x).getString("id").equals(skuInformation.getJSONObject("specs").getString("size"))) {
										sizeLabel = sizes_information.getJSONObject(x).getString("label");
									}
								}

								if(!sizeLabel.isEmpty()) name = name + " (tamanho " + sizeLabel + ")";

							}

						}


						Integer stock = null;

						boolean available = skuInformation.getString("status").equals("available");

						Product product = new Product();
						product.setSeedId(this.session.getSeedId());
						product.setUrl(this.session.getUrl());
						product.setInternalId(internalID);
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

				}
			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}

		return products;

	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.startsWith("http://www.netshoes.com.br/produto/");
	}
}
