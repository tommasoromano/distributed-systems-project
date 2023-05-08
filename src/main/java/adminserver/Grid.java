package adminserver;

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
  
}
