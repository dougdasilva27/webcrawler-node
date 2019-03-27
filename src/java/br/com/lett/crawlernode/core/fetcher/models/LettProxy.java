package br.com.lett.crawlernode.core.fetcher.models;

import java.io.Serializable;
import java.util.UUID;
import org.apache.commons.lang3.SerializationUtils;
import org.json.JSONObject;

public class LettProxy implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = UUID.randomUUID().getMostSignificantBits();
  private String source;
  private String address;
  private Integer port;
  private String location;
  private String user;
  private String pass;

  public LettProxy() {}

  public LettProxy(String source, String address, Integer port, String location, String user, String pass) {
    super();
    this.source = source;
    this.address = address;
    this.port = port;
    this.location = location;
    this.user = user;
    this.pass = pass;
  }

  public LettProxy clone() {
    return SerializationUtils.clone(this);
  }

  @Override
  public String toString() {
    return "Proxy [source=" + source + ", address=" + address + ", port=" + port + ", location=" + location + ", user=" + user + ", pass=" + pass
        + "]";
  }


  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPass() {
    return pass;
  }

  public void setPass(String pass) {
    this.pass = pass;
  }

  public JSONObject toJson() {
    JSONObject proxyJson = new JSONObject();

    if (this.user != null) {
      proxyJson.put("user", this.user);
    }

    if (this.pass != null) {
      proxyJson.put("pass", this.pass);
    }

    if (this.address != null) {
      proxyJson.put("host", this.address);
    }

    if (this.port != null) {
      proxyJson.put("port", this.port);
    }

    if (this.source != null) {
      proxyJson.put("source", this.source);
    }

    if (this.location != null) {
      proxyJson.put("location", this.location);
    }

    return proxyJson;
  }
}
