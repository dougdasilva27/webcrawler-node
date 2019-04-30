package br.com.lett.crawlernode.core.models;

import org.json.JSONObject;
import br.com.lett.crawlernode.exceptions.BuyBoxSellerException;

public class BuyBoxSeller {

  private String id;
  private String name;
  private Float price;
  private Integer position;

  public BuyBoxSeller(String id, String name, Float price, Integer position) throws BuyBoxSellerException {
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

  public int getPosition() {
    return position;
  }

  public JSONObject toJson() {
    JSONObject sellerJSON = new JSONObject();

    sellerJSON.put("id", this.id);
    sellerJSON.put("name", this.id);
    sellerJSON.put("price", this.price);
    sellerJSON.put("position", this.position);

    return sellerJSON;
  }
}
