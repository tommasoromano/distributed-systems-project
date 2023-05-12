package utils;

import adminserver.RegisteredRobot;

import java.util.ArrayList;
import java.util.List;

public class District {
  private int id;
  private Grid grid;
  private List<RegisteredRobot> registeredRobots;
  public District(int id, Grid grid) {
    this.id = id;
    this.grid = grid;
    this.registeredRobots = new ArrayList<RegisteredRobot>();
  }
  public int getId() {
    return this.id;
  }
  public Grid getGrid() {
    return this.grid;
  }
  public Position getOrigin() {
    return this.grid.getOrigin();
  }
  public int getWidth() {
    return this.grid.getWidth();
  }
  public int getHeight() {
    return this.grid.getHeight();
  }
  public List<RegisteredRobot> getRegisteredRobots() {
    return this.registeredRobots;
  }
  public void removeRobot(int id) {
    for (RegisteredRobot registeredRobot : this.registeredRobots) {
      if (registeredRobot.getId() == id) {
        this.registeredRobots.remove(registeredRobot);
        return;
      }
    }
  }
  public Position getRandomPosition() {
    return this.grid.getRandomPosition();
  }
  public void addRobot(RegisteredRobot robot) {
    for (RegisteredRobot registeredRobot : this.registeredRobots) {
      if (registeredRobot.getId() == robot.getId()) {
        return;
      }
    }
    registeredRobots.add(robot);
  }
  public List<RegisteredRobot> getRobots() {
    return this.registeredRobots;
  }

  public String getRepresentationAtPosition(Position position) {
    if (!this.grid.isPositionInGrid(position)) {
      return " ";
    }
    for (RegisteredRobot registeredRobot : this.registeredRobots) {
      if (registeredRobot.getPosition().equals(position)) {
        return "R";
      }
    }
    return "·";
  }

  // public String[][] getGridRepresentation() {
  //   String[][] gridRepresentation = new String[this.grid.getHeight()][this.grid.getWidth()];
  //   for (int i = 0; i < this.grid.getHeight(); i++) {
  //     for (int j = 0; j < this.grid.getWidth(); j++) {
  //       int x = j + this.grid.getOrigin().getX();
  //       int y = i + this.grid.getOrigin().getY();
  //       // int x = this.grid.getRelativePosition(new Position(j, i));
  //       // int y = this.grid.getRelativePosition(new Position(j, i));
  //       if (!this.grid.isPositionInGrid(x,y)) {
  //         gridRepresentation[i][j] = " ";
  //         continue;
  //       }
  //       gridRepresentation[i][j] = "·";
  //     }
  //   }
  //   // for (RegisteredRobot registeredRobot : this.registeredRobots) {
  //   //   Position position = registeredRobot.getPosition();
  //   //   if (this.grid.isPositionInGrid(position)) {
  //   //     int x = position.getX() - this.grid.getOrigin().getX();
  //   //     int y = position.getY() - this.grid.getOrigin().getY();
  //   //     // int x = this.grid.getAbsolutePosition(new Position(j, i));
  //   //     // int y = this.grid.getAbsolutePosition(new Position(j, i));
  //   //     gridRepresentation[position.getY() - this.grid.getOrigin().getY()][position.getX() - this.grid.getOrigin().getX()] = "R";
  //   //     gridRepresentation[y][x] = "R";
  //   //   }
  //   // }
  //   return gridRepresentation;
  // }
  
}
