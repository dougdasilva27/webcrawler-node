package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import br.com.lett.crawlernode.util.MathCommonsMethods;

/************************************************************************************************************************************************************************************
 * Crawling notes (11/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) if the sku is unavailable, it's price is not displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ex1 (available, multiple images): http://www.ambientair.com.br/split-hi-wall/split-hi-wall-midea-liva-eco-9000-btuh-frio-220v-42mfcb09m5-38kcv09m5.html
 * ex2 (unavailable): http://www.ambientair.com.br/condicionadores-de-ar-janela/ar-condicionado-portatil-springer-nova-mpn12crv2-12000-btuh-frio-c-contr-remoto-220v.html 
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilAmbientairCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.ambientair.com.br/";
	private final String PROTOCOL = "http://";

	public BrasilAmbientairCrawler(Session session) {
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

			// InternalId
			String internalId = crawlInternalId(doc);

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String name = crawlName(doc);

			// Price
			Float price = crawlMainPagePrice(doc);

			Prices prices = crawlPrices(doc);

			// Availability
			boolean available = crawlAvailability(doc);

			// Categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Primary image
			String primaryImage = crawlPrimaryImage(doc);

			// Secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// Description
			String description = crawlDescription(doc);

			// Stock
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = crawlMarketplace(doc);

			// Creating the product
			Product product = new Product();

			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
			product.setAvailable(available);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);

			products.add(product);

		} else {
			Logging.printLogTrace(logger, "Not a product page " + this.session.getOriginalURL());

		}

		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		if ( document.select("#descricao").first() != null ) return true;
		return false;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(Document document) {
		String internalId = null;
		Elements internalIdElements = document.select("input[name=variacao]");

		if (internalIdElements.size() > 0) {
			for (Element id : internalIdElements) {
				if (id.attr("value") != null && !id.attr("value").isEmpty()) {
					internalId = id.attr("value").trim();
					break;
				}
			}		
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h2.produto").first();

		if (nameElement != null) {
			name = sanitizeName(nameElement.text());
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select(".preco .precoPor").first();

		if (mainPagePriceElement != null) {
			price = Float.parseFloat( mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}

	private Prices crawlPrices(Document document) {
		Prices prices = new Prices();

		// bank slip
		Element bankSlipLineElement = document.select("#descontoParcelamento .colunaPagamento ul.tabVista li").first();
		if (bankSlipLineElement != null) {
			String bankSlipText = bankSlipLineElement.text().toLowerCase();
			Element bankSlipPriceTextElement = bankSlipLineElement.select("span.precoDesc").first();

			if (!bankSlipText.isEmpty() && bankSlipPriceTextElement != null) {
				String bankSlipPriceText = bankSlipPriceTextElement.text();
				if (bankSlipText.contains("boleto")) {
					prices.insertBankTicket(MathCommonsMethods.parseFloat(bankSlipPriceText));
				}
			}
		}

		// installments
		Element skuPayments = document.select(".parcelamentoProduto").first();
		if (skuPayments != null) {
			Map<Integer, Float> installments = getInstallmentsAndValuesFromElement(skuPayments);
			Elements cardsElements = document.select("#descontoParcelamento .colunaPagamento ul.tabVista li");
			for (int i = 1; i < cardsElements.size(); i++) { // the first one is the bank slip price
				String cardLineText = cardsElements.get(i).text().toLowerCase();
				Element cardPriceTextElement = cardsElements.get(i).select("span.precoDesc").first();

				if (!cardLineText.isEmpty() && cardPriceTextElement != null)  {
					String cardPriceText = cardPriceTextElement.text();

					if (cardLineText.contains(Card.MASTERCARD.toString())) {
						Map<Integer, Float> cardInstallments = new TreeMap<Integer, Float>();

						cardInstallments.put(1, MathCommonsMethods.parseFloat(cardPriceText));
						for (Integer installmentNumber : installments.keySet()) {
							cardInstallments.put(installmentNumber, installments.get(installmentNumber));
						}

						prices.insertCardInstallment(Card.MASTERCARD.toString(), cardInstallments);
					}
					else if (cardLineText.contains(Card.VISA.toString())) {
						Map<Integer, Float> cardInstallments = new TreeMap<Integer, Float>();

						cardInstallments.put(1, MathCommonsMethods.parseFloat(cardPriceText));
						for (Integer installmentNumber : installments.keySet()) {
							cardInstallments.put(installmentNumber, installments.get(installmentNumber));
						}

						prices.insertCardInstallment(Card.VISA.toString(), cardInstallments);
					}
					else if (cardLineText.contains(Card.DINERS.toString())) {
						Map<Integer, Float> cardInstallments = new TreeMap<Integer, Float>();

						cardInstallments.put(1, MathCommonsMethods.parseFloat(cardPriceText));
						for (Integer installmentNumber : installments.keySet()) {
							cardInstallments.put(installmentNumber, installments.get(installmentNumber));
						}

						prices.insertCardInstallment(Card.DINERS.toString(), cardInstallments);
					}
					else if (cardLineText.contains(Card.AMEX.toString()) || cardLineText.contains("american express")) {
						Map<Integer, Float> cardInstallments = new TreeMap<Integer, Float>();

						cardInstallments.put(1, MathCommonsMethods.parseFloat(cardPriceText));
						for (Integer installmentNumber : installments.keySet()) {
							cardInstallments.put(installmentNumber, installments.get(installmentNumber));
						}

						prices.insertCardInstallment(Card.AMEX.toString(), cardInstallments);
					}
					else if (cardLineText.contains(Card.ELO.toString())) {
						Map<Integer, Float> cardInstallments = new TreeMap<Integer, Float>();

						cardInstallments.put(1, MathCommonsMethods.parseFloat(cardPriceText));
						for (Integer installmentNumber : installments.keySet()) {
							cardInstallments.put(installmentNumber, installments.get(installmentNumber));
						}

						prices.insertCardInstallment(Card.ELO.toString(), cardInstallments);
					}
				}
			}
		}


		return prices;
	}


	/**
	 * Opções de parcelamento no cartão
	 *	2x R$ 3.089,50 sem juros 
	 *	3x R$ 2.059,65 sem juros 
	 *	4x R$ 1.544,75 sem juros 
	 *	5x R$ 1.235,80 sem juros 
	 *	6x R$ 1.029,79 sem juros 
	 *	7x R$ 882,67 sem juros 
	 *	8x R$ 772,38 sem juros 
	 *	9x R$ 686,55 sem juros 
	 *	10x R$ 617,90 sem juros
	 *
	 *	The payment options are the same across all card brands.
	 *
	 * @param paymentsElements
	 * @return
	 */
	private Map<Integer, Float> getInstallmentsAndValuesFromElement(Element skuPayments) {
		Elements installmentsLineElements = skuPayments.select("ul.listaParcelamento li");
		Map<Integer, Float> installments = new TreeMap<Integer, Float>();

		for (Element installmentLineElement : installmentsLineElements) {
			Element installmentNumberElement = installmentLineElement.select(".tdParcela").first();
			Element installmentPriceElement = installmentLineElement.select(".tdValparcela").first();
			if (installmentNumberElement != null && installmentPriceElement != null) {
				String installmentNumberText = installmentNumberElement.text();
				String installmentPriceText = installmentPriceElement.text();

				if (!installmentNumberText.isEmpty() && !installmentPriceText.isEmpty()) {
					List<String> numbersFromInstallmentNumberText = MathCommonsMethods.parseNumbers(installmentNumberText);
					installments.put(Integer.parseInt(numbersFromInstallmentNumberText.get(0)), MathCommonsMethods.parseFloat(installmentPriceText));
				}
			}
		}

		return installments;
	}

	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(".produtoIndisponivel").first();
		if (notifyMeElement != null) return false;
		return true;
	}

	private JSONArray crawlMarketplace(Document document) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select("#foto #produto img").first();

		if (primaryImageElement != null) {
			primaryImage = PROTOCOL + "www.ambientair.com.br" + primaryImageElement.attr("data-zoom-image").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#foto #extras ul li a");

		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			secondaryImagesArray.put( PROTOCOL + "www.ambientair.com.br" + imagesElement.get(i).attr("data-zoom-image").trim() );
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select("#breadcrumb a");

		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
			categories.add( elementCategories.get(i).text().trim() );
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
		Element descriptionElement = document.select("#abas1").first();
		Element specElement = document.select("#abas2").first();

		if (descriptionElement != null) description = description + descriptionElement.html();
		if (specElement != null) description = description + specElement.html();

		return description;
	}

	/**************************
	 * Specific manipulations *
	 **************************/

	private String sanitizeName(String name) {
		return name.replace("'","").replace("’","").trim();
	}


}
