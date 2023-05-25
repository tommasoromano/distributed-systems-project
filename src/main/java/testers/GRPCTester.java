package testers;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import protos.network.NetworkMessage;
import protos.network.NetworkResponse;
import protos.network.NetworkServiceGrpc;
import protos.network.NetworkServiceGrpc.NetworkServiceBlockingStub;

public class GRPCTester {
  public static void main(String[] args) {
    sendMessage();
  }

  public static void sendMessage() {

    int portNumber = 1234;
    int robotId = 9876;

    final ManagedChannel channel =
        ManagedChannelBuilder.forTarget("localhost:"+portNumber).usePlaintext().build();

    NetworkServiceBlockingStub stub = NetworkServiceGrpc.newBlockingStub(channel);

    NetworkMessage request = NetworkMessage.newBuilder()
      .setMessageType("msgtype").setSenderId(robotId).setSenderPort(portNumber).setTimestamp(System.currentTimeMillis()).setAdditionalPayload("none").build();

    System.out.println("gRPC: Sending message: "+request+", to port:"+portNumber);

    NetworkResponse response = stub.sendNetworkMessage(request);

    System.out.println("gRPC: Response: "+response);

    channel.shutdown();
  }
}
