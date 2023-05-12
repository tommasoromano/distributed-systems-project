package adminserver.REST;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import utils.City;

import java.io.IOException;

public class AdminServerThread implements Runnable {
    private City city;
    public AdminServerThread(City city) {
        this.city = city;
    }
    @Override
    public void run() {
        try {
            System.out.println("Starting server - Thread PID: " + Thread.currentThread().getId());
            String uri = "http://"+city.getHost()+":"+city.getPort()+"/";
            HttpServer server = HttpServerFactory.create(uri);
            server.start();
            System.out.println("Server started on: "+uri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
