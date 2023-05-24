package robot.communication;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import robot.Robot;

public class GRPCServer implements Runnable {

  @Override
  public void run() {

    try {
      Server server = ServerBuilder.forPort(Robot.getInstance().getPortNumber())
                      .addService(new GRPCServiceImpl())
                      .build();

      server.start();

      System.out.println("gRPC: running on port "+Robot.getInstance().getPortNumber()+"...");
      Robot.getInstance().getCommunication().setConnectedToGRPC(true);

      server.awaitTermination();
    } catch (Exception e) {
      System.out.println("gRPC: Error: " + e.getMessage());
      Robot.getInstance().disconnect();
    }
  }
  
}
