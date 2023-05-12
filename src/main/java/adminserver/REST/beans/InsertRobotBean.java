package adminserver.REST.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InsertRobotBean {
  private int x;
  private int y;
  private List<RobotBean> robots;
  public InsertRobotBean() {
  }
  public InsertRobotBean(int x, int y, List<RobotBean> robots) {
    this.x = x;
    this.y = y;
    this.robots = robots;
  }
  public int getX() {
    return x;
  }
  public void setX(int x) {
    this.x = x;
  }
  public int getY() {
    return y;
  }
  
  public List<RobotBean> getRobots() {
    return robots;
  }
  public void setRobots(List<RobotBean> robots) {
    this.robots = robots;
  }
}
