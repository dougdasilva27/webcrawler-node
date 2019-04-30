package br.com.lett.crawlernode.core.models;

import org.json.JSONObject;
import br.com.lett.crawlernode.exceptions.BuyBoxSellerException;
import models.prices.Prices;

public class SellerV2 {

  private String id;
  private String name;
  private Float price;
  private Prices prices;
  private Integer position;

  public SellerV2(String id, String name, Float price, Prices prices, Integer position) throws BuyBoxSellerException {
    if (id != null) {
      if (!id.isEmpty()) {
        this.id = id;
      } else {
        throw new BuyBoxSellerException("ID", id);
      }
    } else {
      throw new BuyBoxSellerException("ID");
    }

    if (name != null) {
      if (!name.isEmpty()) {
        this.name = name;
      } else {
        throw new BuyBoxSellerException("NAME", name);
      }
    } else {
      throw new BuyBoxSellerException("NAME");
    }

    if (price != null) {
      if (price > 0f) {
        this.price = price;
      } else {
        throw new BuyBoxSellerException("PRICE", price);
      }
    } else {
      throw new BuyBoxSellerException("PRICE");
    }

    if (prices != null) {
      if (!prices.isEmpty()) {
        this.prices = prices;
      } else {
        throw new BuyBoxSellerException("PRICES", prices.toJSON());
      }
    } else {
      throw new BuyBoxSellerException("PRICES");
    }

    if (position != null) {
      this.position = position;
    } else {
      throw new BuyBoxSellerException("POSITION");
    }
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Float getPrice() {
    return price;
  }

  public Prices getPrices() {
    return prices;
  }

  public int getPosition() {
    return position;
  }

  public JSONObject toJson() {
    JSONObject sellerJSON = new JSONObject();

    sellerJSON.put("id", this.id);
    sellerJSON.put("name", this.id);
    sellerJSON.put("price", this.price);
    sellerJSON.put("prices", this.prices.toJSON());
    sellerJSON.put("position", this.position);

    return sellerJSON;
  }
}
