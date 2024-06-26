package robot.network;

import java.util.ArrayList;
import java.util.List;

public class NetworkQueue {

  private List<QueueNode> queue;
  public NetworkQueue() {
    this.queue = new ArrayList<>();
  }
  /**
   * Add a node to the queue, ordered by timestamp
   * @param node
   */
  public synchronized void add(QueueNode node) {
    for (int i = 0; i < this.queue.size(); i++) {
      if (node.getTimestamp() > this.queue.get(i).getTimestamp()) {
        this.queue.add(i, node);
        return;
      }
    }
    this.queue.add(node);
  }
  public synchronized void remove(int id) {
    this.queue.removeIf((node) -> node.getRobot().getId() == id);
  }
  public synchronized List<QueueNode> readAndClear() {
    List<QueueNode> tmp = new ArrayList<>(this.queue);
    this.queue.clear();
    return tmp;
  }
  public synchronized int size() {
    return this.queue.size();
  }
  public synchronized boolean contains(int id) {
    for (QueueNode node : this.queue) {
      if (node.getRobot().getId() == id) {
        return true;
      }
    }
    return false;
  }
  public synchronized String queueNodesToString() {
    String s = "[ ";
    for (QueueNode node : this.queue) {
      s += node.getRobot().getId() + " ";
    }
    return s + "]";
  }
}
