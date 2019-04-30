package br.com.lett.crawlernode.core.models;

import org.json.JSONObject;

public class BuyBoxSeller {

  private String id;
  private String name;
  private Float price;
  private int position;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Float getPrice() {
    return price;
  }

  public void setPrice(Float price) {
    this.price = price;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public JSONObject toJson() {
    JSONObject sellerJSON = new JSONObject();
    sellerJSON.put("id", this.id);
    sellerJSON.put("name", name);
    sellerJSON.put("price", price);
    sellerJSON.put("position", this.position);

    return sellerJSON;
  }
}
