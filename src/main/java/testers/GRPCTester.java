package testers;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import protos.network.NetworkMessage;
import protos.network.NetworkResult;
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
      .setMsg("msg").setId(robotId).setTs(System.currentTimeMillis()).build();

    System.out.println("gRPC: Sending message: "+request.getMsg()+", to port:"+portNumber);

    NetworkResult response = stub.sendNetworkMessage(request);

    System.out.println("gRPC: Response: "+response.getResMsg());

    channel.shutdown();
  }
}
