package robot.communication;

import io.grpc.stub.StreamObserver;
import protos.network.NetworkMessage;
import protos.network.NetworkResponse;
import protos.network.NetworkServiceGrpc.NetworkServiceImplBase;
import robot.Robot;

public class GRPCServiceImpl extends NetworkServiceImplBase {
  @Override
  public void sendNetworkMessage(NetworkMessage request, StreamObserver<NetworkResponse> responseObserver) {

    System.out.println("gRPC Service: received "+request.getMessageType()+" message from robot "+request.getSenderId());

    NetworkResponse response = Robot.getInstance().getCommunication()
      .createResponseForRobotMessage(request);

    responseObserver.onNext(response);

    responseObserver.onCompleted();
  }
}
