package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;

public class RiodejaneiroSuperprixCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.superprix.com.br/";

	public RiodejaneiroSuperprixCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();

		// Não pegaremos as páginas que contém "?ftr=" pois elas são categorias filtradas, que não nos interessam.
		return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if ( isProductPage(session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Id interno
			Element span = doc.select(".productDescriptionShort").first();
			if(span==null){
				span = doc.select("div.short-description span").first();
			}
			String id = span.text().replaceAll("[^0-9,]+", "").trim();
			String internalID = Integer.toString(Integer.parseInt(id));

			// Nome
			Elements elementName = doc.select(".productName");
			String name = elementName.text().replace("'", "").trim();

			// Preço
			Element elementPrice = doc.select(".skuBestPrice").first();
			Float price = null;
			if (elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "")
						.replaceAll(",", "."));
			}

			// Disponibilidade
			Boolean available = true;

			// Categories
			ArrayList<String> categories = this.crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Imagens
			String primaryImage = "";
			Element element_foto = doc.select(".image-zoom").first();
			if(element_foto != null){
				primaryImage = element_foto.attr("href").trim();
	
				if(primaryImage.contains("produto_sem_foto")){
					primaryImage = null;
				}
			}

			String secondaryImages = null;


			// Descrição
			String description = crawlDescription(doc);

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			// Prices
			Prices prices = crawlPrices(price);
			
			// Create a product
			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalID);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (url.endsWith("/p"));
	}
	
	private ArrayList<String> crawlCategories(Document document) {
		Elements elementCategories = document.select(".bread-crumb a");
		ArrayList<String> categories = new ArrayList<>();

		for(int i = 1; i < elementCategories.size(); i++) { // starts with index 1 because the first item is the home page
			Element e = elementCategories.get(i);
			String tmp = e.text().toString();

			categories.add(tmp);
		}

		return categories;
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}
	
	private String crawlDescription(Document document) {
		String description = "";
		Element elementProductDetails = document.select(".prod-descricao").first();
		Element elementProductCarac = document.select("#caracteristicas").first();
		Element elementProductTabela = document.select(".tabela-nutricional").first();
		
		if(elementProductDetails != null) {
			description = description + elementProductDetails.html();
		}
		if(elementProductCarac != null) {
			description = description + elementProductCarac.html();
		}
		if(elementProductTabela != null) {
			description = description + elementProductTabela.html();
		}

		return description;
	}	
	
	/**
	 * In this market, installments not appear in product page
	 * Has no bank slip payment method
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price){
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			installmentPriceMap.put(1, price);

			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
		}

		return prices;
	}
}