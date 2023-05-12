package utils;

public class Grid {
  private Position origin;
  private int width;
  private int height;
  public Grid(Position origin, int width, int height) {
    this.origin = origin;
    this.width = width;
    this.height = height;
  }

  public Position getRandomPosition() {
    int x = this.origin.getX() + ((int) (Math.random() * this.width));
    int y = this.origin.getY() + ((int) (Math.random() * this.height));
    return new Position(x, y);
  }

  public Position getOrigin() {
    return this.origin;
  }
  public int getWidth() {
    return this.width;
  }
  public int getHeight() {
    return this.height;
  }

  public boolean isPositionInGrid(Position position) {
    return position.getX() >= this.origin.getX() 
    && position.getX() < this.origin.getX() + this.width 
    && position.getY() >= this.origin.getY() 
    && position.getY() < this.origin.getY() + this.height;
  }
  public boolean isPositionInGrid(int x, int y) {
    return x >= this.origin.getX() 
    && x < this.origin.getX() + this.width 
    && y >= this.origin.getY() 
    && y < this.origin.getY() + this.height;
  }

  public Position getRelativePosition(Position position) {
    return new Position(position.getX() - this.origin.getX(), position.getY() - this.origin.getY());
  }
  public Position getAbsolutePosition(Position position) {
    return new Position(position.getX() + this.origin.getX(), position.getY() + this.origin.getY());
  }
  
  
}
