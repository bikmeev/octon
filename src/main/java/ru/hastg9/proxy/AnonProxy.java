package ru.hastg9.proxy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AnonProxy implements Runnable{

    private final Logger LOGGER = LogManager.getLogger(Class.class);

    public final int PORT;
    private final List<ProxyClient> servers = new CopyOnWriteArrayList<>();

    public AnonProxy(int PORT) {
        this.PORT = PORT;
    }

    @Override
    public void run() {
        LOGGER.info("Proxy socket running at port: " + PORT);
        try(ServerSocket serv = new ServerSocket(PORT)) {
            while(true) {
                Socket socket = serv.accept();
                ProxyClient server = new ProxyClient(socket, this);
                add(server);
                new Thread(server).start();
            }
        }catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            LOGGER.trace(ex.getMessage(), ex);
        }
    }

    public void add(ProxyClient client) {
        servers.add(client);
    }

    public void remove(ProxyClient client) {
        servers.remove(client);
    }

    public boolean contains(String address) {
        return servers.stream().filter(client -> client.address.equals(address)).count() > 1;
    }

    public List<ProxyClient> getClients() {
        return servers;
    }

    public int count() {
        return servers.size();
    }

}
