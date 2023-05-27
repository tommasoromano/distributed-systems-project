package robot.communication.network;

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

      // Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      //     System.out.println("Shutting down gRPC server");
      //     MyServiceServer.this.stop();
      //     System.out.println("Server shut down");
      // }));

      server.awaitTermination();
    } catch (Exception e) {
      System.out.println("gRPC: Error: " + e.getMessage());
      Robot.getInstance().disconnect();
    }
  }
  
  // public void stop() {
  //   if (server != null) {
  //     server.shutdown();
  //   }
  // }

}
