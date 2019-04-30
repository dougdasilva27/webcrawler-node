package br.com.lett.crawlernode.core.models;

import java.util.List;
import br.com.lett.crawlernode.util.DateUtils;
import models.Marketplace;
import models.prices.Prices;

public class ProductBuilder {

  private String url;
  private String internalId;
  private String internalPid;
  private String name;
  private Float price;
  private Prices prices;
  private boolean available;
  private String category1;
  private String category2;
  private String category3;
  private String primaryImage;
  private String secondaryImages;
  private String description;
  private Marketplace marketplace;
  private Integer stock;
  private String ean;
  private List<String> eans;
  private List<BuyBoxSeller> mainBuyBox;
  private List<BuyBoxSeller> allSellers;

  public static ProductBuilder create() {
    return new ProductBuilder();
  }

  public ProductBuilder setUrl(String url) {
    this.url = url;
    return this;
  }

  public ProductBuilder setInternalId(String internalId) {
    this.internalId = internalId;
    return this;
  }

  public ProductBuilder setInternalPid(String internalPid) {
    this.internalPid = internalPid;
    return this;
  }

  public ProductBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public ProductBuilder setPrice(Float price) {
    this.price = price;
    return this;
  }

  public ProductBuilder setAvailable(boolean available) {
    this.available = available;
    return this;
  }

  public ProductBuilder setCategory1(String category1) {
    this.category1 = category1;
    return this;
  }

  public ProductBuilder setCategory2(String category2) {
    this.category2 = category2;
    return this;
  }

  public ProductBuilder setCategory3(String category3) {
    this.category3 = category3;
    return this;
  }

  public ProductBuilder setPrimaryImage(String primaryImage) {
    this.primaryImage = primaryImage;
    return this;
  }

  public ProductBuilder setSecondaryImages(String secondaryImages) {
    this.secondaryImages = secondaryImages;
    return this;
  }

  public ProductBuilder setDescription(String description) {
    this.description = description;
    return this;
  }

  public ProductBuilder setMarketplace(Marketplace marketplace) {
    this.marketplace = marketplace;
    return this;
  }

  public ProductBuilder setStock(Integer stock) {
    this.stock = stock;
    return this;
  }

  public ProductBuilder setPrices(Prices prices) {
    this.prices = prices;
    return this;
  }

  public ProductBuilder setEan(String ean) {
    this.ean = ean;
    return this;
  }

  public ProductBuilder setEans(List<String> eans) {
    this.eans = eans;
    return this;
  }

  public ProductBuilder setMainBuyBox(List<BuyBoxSeller> mainBuyBox) {
    this.mainBuyBox = mainBuyBox;
    return this;
  }

  public ProductBuilder setAllSellers(List<BuyBoxSeller> allSellers) {
    this.allSellers = allSellers;
    return this;
  }

  public Product build() {
    Product product = new Product();

    product.setUrl(this.url);
    product.setInternalId(this.internalId);
    product.setInternalPid(this.internalPid);
    product.setName(this.name);
    product.setPrice(this.price);
    product.setPrices(this.prices);
    product.setAvailable(this.available);
    product.setCategory1(this.category1);
    product.setCategory2(this.category2);
    product.setCategory3(this.category3);
    product.setPrimaryImage(this.primaryImage);
    product.setSecondaryImages(this.secondaryImages);
    product.setDescription(this.description);
    product.setStock(this.stock);
    product.setMarketplace(this.marketplace);
    product.setEan(this.ean);
    product.setEans(this.eans);
    product.setMainBuyBox(this.mainBuyBox);
    product.setAllSellers(this.allSellers);

    // Timestamp is only created here, there is no public method to set timestamp
    product.setTimestamp(DateUtils.newTimestamp());

    return product;
  }

}
