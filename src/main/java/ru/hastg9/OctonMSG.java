package ru.hastg9;

import ru.hastg9.api.socket.IServer;
import ru.hastg9.messenger.UserManager;
import ru.hastg9.proxy.AnonProxy;
import ru.hastg9.socket.AnonServer;

public class OctonMSG {
    private static IServer server;
    private static AnonProxy proxy;
    private static UserManager manager;

    public static void main(String[] args) {

        manager = new UserManager();

        initServer();

    }

    public static void initServer() {
        server = new AnonServer(52741);
        new Thread(server).start();

        proxy = new AnonProxy(4557);
        new Thread(proxy).start();
    }


    public static UserManager getManager() {
        return manager;
    }

    public static IServer getServer() {
        return server;
    }

}
