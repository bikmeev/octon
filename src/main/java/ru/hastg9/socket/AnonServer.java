package ru.hastg9.socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.hastg9.api.socket.IConnection;
import ru.hastg9.api.socket.IServer;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AnonServer implements IServer {

    private final Logger LOGGER = LogManager.getLogger(Class.class);

    public final int PORT;
    private final List<IConnection> servers = new CopyOnWriteArrayList<>();

    public AnonServer(int PORT) {
        this.PORT = PORT;
    }

    @Override
    public void run() {
        LOGGER.info("Initializing the server socket");
        try(ServerSocket serv = new ServerSocket(PORT)) {
            while(true) {
                Socket socket = serv.accept();
                IConnection server = new AnonConnection(socket, this);
                add(server);
                new Thread(server).start();
            }
        }catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            LOGGER.trace(ex.getMessage(), ex);
        }
    }

    @Override
    public void add(IConnection connection) {
        servers.add(connection);
    }

    @Override
    public void remove(IConnection connection) {
        servers.remove(connection);
    }

    @Override
    public IConnection get(int id) {
        for (IConnection s : servers) {
            if(s.getID() == id) return s;
        }
        return null;
    }

    @Override
    public int getLast() {
        if(servers.isEmpty()) return 0;
        else {
            return servers.get(servers.size() - 1).getID();
        }
    }

    @Override
    public List<IConnection> getConnections() {
        return servers;
    }

    @Override
    public int count() {
        return servers.size();
    }

    @Override
    public int getPort() {
        return PORT;
    }

}
