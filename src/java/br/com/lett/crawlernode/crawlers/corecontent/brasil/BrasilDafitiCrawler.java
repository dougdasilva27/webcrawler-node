package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

public class BrasilDafitiCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.dafiti.com.br/";

	public BrasilDafitiCrawler(Session session) {
		super(session);
	}	

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Nome
			Elements elementPreName = doc.select("h1.product-name");
			String preName = elementPreName.text().replace("'", "").replace("’", "").trim();

			// Preço
			Float price = null;
			Element elementPrice = doc.select(".catalog-detail-price-value").first();
			if (elementPrice == null) {
				price = null;
			} else {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Categorias
			Elements elementCategories = doc.select(".container.product-page .row ul.breadcrumb2").first().select("li");
			String category1 = null;
			String category2 = null;
			String category3 = null;

			if (elementCategories.size() > 1) {
				category1 = elementCategories.get(1).text();
			}
			if (elementCategories.size() > 2) {
				category2 = elementCategories.get(2).text();
			}
			if (elementCategories.size() > 3) {
				category3 = elementCategories.get(3).text();
			}

			// Imagem primária e imagens secundárias
			Elements elementPrimaryImage = doc.select(".gallery-thumbs ul.carousel-items").select("a");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for (Element e : elementPrimaryImage) {

				if (primaryImage == null) {
					primaryImage = e.attr("data-img-zoom");
				} else {
					secondaryImagesArray.put(e.attr("data-img-zoom"));
				}

			}

			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Elements elementDescription = doc.select(".product-information-content");
			description = elementDescription.first().text().replace(".", ".\n").replace("'", "").replace("’", "").trim();

			Element elementSku = doc.select("#add-to-cart input[name=p]").first();

			try {
				String sku = elementSku.attr("value");

				// Pegando os produtos usando o endpoint da Dafiti

				URL fetchSkuUrl = new URL(
						"http://www.dafiti.com.br/catalog/detailJson?sku=" + sku + "&_=1439492531368");
				URLConnection con = fetchSkuUrl.openConnection();
				InputStream in = con.getInputStream();
				String encoding = con.getContentEncoding();
				encoding = (encoding == null ? "UTF-8" : encoding);
				JSONArray sizes = new JSONObject(IOUtils.toString(in, encoding)).getJSONArray("sizes");

				/*
				 * Cada tamanho do calçado é um produto diferente. Quando
				 * todos os tamanhos estão fora de estoque, o preço do
				 * produto não é exibido. Porém, se pelo menos um dos
				 * tamanhos do calçado está disponível, então o preço é
				 * exibido. Anteriormente o crawler mostrava preço, mesmo no
				 * caso em que todos os tamanhos estavam indisponíveis,
				 * porque o elemento html continuava existindo, apesar de
				 * não ser exibido. Optamos por averiguar se todos os
				 * tamanhos do calçado estão indisponíveis. Caso positivo, o
				 * preço fica null. Se houver pelo menos um tamanho
				 * disponível, então o crawler deixa o mesmo preço para
				 * todos os tamanhos.
				 */

				boolean availability = false;

				for (int i = 0; i < sizes.length(); i++) {
					Integer stock = Integer.parseInt(sizes.getJSONObject(i).get("stock").toString());
					if (stock > 0) {
						availability = true;
						break;
					}
				}

				if (availability == false) {
					price = null;
				}

				/*
				 * Pegar o restante das informações usando os objetos JSON
				 * vindos do endpoint da dafit
				 */
				for (int i = 0; i < sizes.length(); i++) {

					// ID interno
					String internalId = sizes.getJSONObject(i).getString("sku");

					// Pid
					String internalPid = internalId.split("-")[0];

					// Nome - pré-nome pego anteriormente, acrescido do tamanho do sapato
					String name = preName + " (tamanho " + sizes.getJSONObject(i).getString("name") + ")";

					// Estoque
					Integer stock = Integer.parseInt(sizes.getJSONObject(i).get("stock").toString());

					// Disponibilidade
					boolean available = (stock > 0);

					// Marketplace
					JSONArray marketplace = null;

					Product product = new Product();
					product.setUrl(this.session.getOriginalURL());
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
			} catch (Exception e1) {
				e1.printStackTrace();
			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}
		
		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		return (document.select(".container.product-page").first() != null);
	}
}
