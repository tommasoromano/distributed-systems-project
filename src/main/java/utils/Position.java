package utils;

public class Position {
  private int x;
  private int y;
  public Position(int x, int y) {
    this.x = x;
    this.y = y;
  }
  public int getX() {
    return this.x;
  }
  public int getY() {
    return this.y;
  }
  @Override
  public String toString() {
    return "(" + this.x + ", " + this.y + ")";
  }
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!Position.class.isAssignableFrom(obj.getClass())) {
      return false;
    }
    final Position other = (Position) obj;
    if (this.x != other.x) {
      return false;
    }
    if (this.y != other.y) {
      return false;
    }
    return true;
  }
  
}
