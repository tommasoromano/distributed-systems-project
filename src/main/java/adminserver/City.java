package adminserver;

import java.util.ArrayList;
import java.util.List;

public class City {
  private int id;
  private String name;
  private String host;
  private int port;
  private List<District> districts;
  public City(int id, String name, String host, int port, 
    List<District> districts 
    ) {
    this.id = id;
    this.name = name;
    this.host = host;
    this.port = port;
    this.districts = districts;
  }
  public int getId() {
    return this.id;
  }
  public String getName() {
    return this.name;
  }
  public String getHost() {
    return this.host;
  }
  public int getPort() {
    return this.port;
  }
  public List<District> getDistricts() {
    return this.districts;
  }
  public District getDistrictById(int id) {
    for (District district : this.districts) {
      if (district.getId() == id) {
        return district;
      }
    }
    return null;
  }

  public static City greenfieldCity = new City(
    1, 
    "Greenfield", 
    "localhost", 
    6789, 
    new ArrayList<District>(){
    {
      add(new District(1, new Grid(new Position(0, 0), 5, 5)));
      add(new District(2, new Grid(new Position(0, 5), 5, 5)));
      add(new District(3, new Grid(new Position(5, 0), 5, 5)));
      add(new District(4, new Grid(new Position(5, 5), 5, 5)));
    }
  });

  public static City[] cities = new City[] {
    greenfieldCity
  };

  public static City getCityById(int id) {
    for (City city : cities) {
      if (city.getId() == id) {
        return city;
      }
    }
    return null;
  }

  public static boolean isValidCityId(int id) {
    return getCityById(id) != null;
  }

}
