package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class BrasilBifarmaCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.bifarma.com.br/";

	public BrasilBifarmaCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if (isProductPage(doc)) {

			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// internalId
			Element elementID = doc.select(".detalhe_produto_informacao .mini_info .left").first();
			String internalID = null;
			if (elementID != null) {
				String textAll = elementID.text();
				textAll.replaceAll("\\s+", "");
				int begin = textAll.indexOf(':') + 1;
				internalID = textAll.substring(begin).trim();
			}

			// name
			Element elementName = doc.select(".detalhe_produto_informacao .nome_icone .left").first();
			String name = elementName.text().trim();

			// price
			Float price = null;
			Element elementPrice = doc.select(".boxes .left .produto_preco").first();
			if (elementPrice == null) {
				elementPrice = doc.select(".price-box .special-price .price").first();
			}
			if (elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// categories
			Elements elementCategories = doc.select(".breadcrumb p a");
			String category1;
			String category2;
			String category3;
			String[] cat = new String[3];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			int j = 0;

			for (int i = 0; i < elementCategories.size(); i++) {

				Element e = elementCategories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "");
				j++;

				if (j > 2)
					break;

			}
			category1 = cat[1];
			category2 = cat[2];
			category3 = null;

			// Primary image
			String primaryImage = "";
			String tmp = "";
			Elements elementPrimaryImage = doc.select("#detalhe_produto_vitrine_principal a img");
			tmp = elementPrimaryImage.first().attr("src").trim();

			if (tmp.contains("indisponivel"))
				primaryImage = "";

			if (tmp.contains("/Ampliada/")) {
				primaryImage = tmp.replace("/Ampliada/", "/Super/");
			}

			primaryImage = "http://www.bifarma.com.br" + primaryImage;

			// Secondary images
			String secondaryImages = null;
			Elements elementSecondaryImages = doc.select("#detalhe_produto_thumbs ul a");
			JSONArray secondaryImagesArray = new JSONArray();

			if (elementSecondaryImages.size() > 1) {
				for (int i = 0; i < elementSecondaryImages.size(); i++) {
					Element e = elementSecondaryImages.get(i);
					String attrRel = e.attr("rel");
					int begin = attrRel.indexOf("largeimage:") + 11;
					String tmpImage = attrRel.substring(begin);
					tmpImage = tmpImage.replace("\'", " ").replace('}', ' ').trim();
					tmpImage = "http://www.bifarma.com.br" + tmpImage;
					if (!tmpImage.equals(primaryImage)) {
						secondaryImagesArray.put(tmpImage);
					}
				}
			}
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descricao
			String description = "";
			Element elementProductInformation = doc.select(".descricao_produto").first();
			Element elementWarning = doc.select(".advertencia").first();
			Element elementOtherInformations = doc.select(".outras_infos").first();

			if (elementProductInformation != null) {
				description = description + elementProductInformation.html();
			}
			if (elementWarning != null) {
				description = description + elementWarning.html();
			}
			if (elementOtherInformations != null) {
				description = description + elementOtherInformations.html();
			}

			// Disponibilidade
			boolean available = true;
			Element buttonUnavailable = doc.select(".detalhe_produto_informacao .boxes .right .bt_avise_me").first();
			if (buttonUnavailable != null) {
				available = false;
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;
			
			// Prices
			Prices prices = crawlPrices(doc, price);
			
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

	private boolean isProductPage(Document document) {

		Elements selectedElements = null;
		String[] productUrlFeatures = {"#conteudo_produto", "#detalhe_produto", ".detalhe_produto_informacao", ".detalhe_produto_informacao .mini_info .left"};
		int size = productUrlFeatures.length;

		for(int i = 0; i < size; i++) {
			selectedElements = document.select( productUrlFeatures[i] );
			if(selectedElements == null || selectedElements.size() == 0) {
				return false;
			}
		}

		return true;

	}
	
	/**
	 * Example when has installments:
	 * ou 4X de R$ 21,48 (total cartão a prazo R$ 85,92)
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();
		
		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			
			prices.insertBankTicket(price);
			installmentPriceMap.put(1, price);
			
			Elements installments = doc.select(".boxes .left > p");
			
			for(Element e : installments){
				String text = e.ownText().trim().toLowerCase();
				
				if(text.startsWith("ou")){
					int x = text.indexOf("x");
					int y = text.indexOf("(", x+1);
					
					Integer installment = Integer.parseInt(text.substring(0, x).replaceAll("[^0-9]", ""));
					Float value = MathCommonsMethods.parseFloat(text.substring(x+1, y));
					
					installmentPriceMap.put(installment, value);
					
					break;
				}
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
		}
		
		return prices;
	}
}