package utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import adminserver.RegisteredRobot;

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
  public District getDistrictByPosition(Position position) {
    for (District district : this.districts) {
      if (district.hasPosition(position)) {
        return district;
      }
    }
    return null;
  }
  public Position getOrigin() {
    int originX = districts.get(0).getOrigin().getX();
    int originY = districts.get(0).getOrigin().getY();
    for (int i = 1; i < districts.size(); i++) {
      District district = districts.get(i);
      if (district.getOrigin().getX() < originX) {
        originX = district.getOrigin().getX();
      }
      if (district.getOrigin().getY() < originY) {
        originY = district.getOrigin().getY();
      }
    }
    return new Position(originX, originY);
  }
  public String[][] getGridRepresentation() {
    int originX = districts.get(0).getOrigin().getX();
    int originY = districts.get(0).getOrigin().getY();
    int width = districts.get(0).getGrid().getWidth();
    int height = districts.get(0).getGrid().getHeight();
    for (int i = 1; i < districts.size(); i++) {
      District district = districts.get(i);
      if (district.getOrigin().getX() < originX) {
        originX = district.getOrigin().getX();
      }
      if (district.getOrigin().getY() < originY) {
        originY = district.getOrigin().getY();
      }
      int w = district.getGrid().getWidth() + district.getOrigin().getX();
      if (w > width) {
        width = w;
      }
      int h = district.getGrid().getHeight() + district.getOrigin().getY();
      if (h > height) {
        height = h;
      }
    }
    String[][] gridRepresentation = new String[height][width];
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        gridRepresentation[i][j] = "  ";
        for (District district : this.districts) {
          String pos = district.getRepresentationAtPosition(new Position(j, i));
          if (!pos.equals(" ")) {
            gridRepresentation[i][j] = pos+" ";
          }
        }
      }
    }
    return gridRepresentation;
  }
  public String getRepresentation() {
    String[][] gridRepresentation = this.getGridRepresentation();
    String representation = "";
    for (int i = 0; i < gridRepresentation.length; i++) {
      for (int j = 0; j < gridRepresentation[i].length; j++) {
        representation += gridRepresentation[i][j];
      }
      representation += "\n";
    }
    for (District district : this.districts) {
      for (RegisteredRobot robot : district.getRobots()) {
        representation += "\n"+robot.getRepresentation();
      }
    }
    return representation;
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

  public static City selectCityStdInput() {

    while(true) {
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String input;

      System.out.println("Select a city:");
      for (City city : City.cities) {
        System.out.println(city.getId() + ". " + city.getName());
      }

      try {
        input = br.readLine();
        int cityId = Integer.parseInt(input);
        if (City.isValidCityId(cityId)) {
          City city = City.getCityById(cityId);
          System.out.println("Selected city: " 
          + "\n\tName:  " + city.getName()
          + "\n\tId:    " + city.getId());
          return city;
        } else {
          System.out.println("Invalid city id.");
        }
      } catch (Exception e) {
        System.out.println("Invalid input.");
      }
    }
  }

}
