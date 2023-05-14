package ru.hastg9.proxy;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.hastg9.OctonMSG;
import ru.hastg9.encode.EncodeUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class ProxyClient implements Runnable{

    private final Logger LOGGER = LogManager.getLogger(Class.class);

    protected final Socket socket;
    protected AnonProxy proxy;

    protected ProxyConnection client;
    protected ProxyConnection server;

    protected String address;


    public ProxyClient(Socket socket, AnonProxy proxy) {
        this.proxy = proxy;
        this.socket = socket;

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(socket.getInetAddress().getHostAddress().getBytes());
            byte[] digest = md5.digest();

            this.address = Hex.encodeHexString(digest).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error(e.getMessage(), e);
            LOGGER.trace(e.getMessage(), e);
        }

        LOGGER.debug("({}) Incoming connection", address);

    }


    @Override
    public void run() {
        try {

            if(proxy.contains(address)) {
                disconnect();
                return;
            }

            client = new ProxyConnection(socket, this) {
                @Override
                void onClose() {
                    proxy.server.disconnect();

                    LOGGER.debug("({}/CLIENT) Disconnected", address);

                    proxy.proxy.remove(proxy);
                }

                @Override
                boolean validate(String packet) {
                    return packet.getBytes(StandardCharsets.UTF_8).length < MAX_PACKET_SIZE;
                }

                @Override
                public void onReceipt(String obj) {
                    proxy.server.write(EncodeUtils.encode(obj));

                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(1) / 2);
                    } catch (InterruptedException e) {
                        LOGGER.error(e);
                        LOGGER.trace(e.getMessage(), e);
                    }
                }
            };

            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(OctonMSG.getServer().getPort()));

            server = new ProxyConnection(socket, this) {
                @Override
                void onClose() {
                    proxy.client.disconnect();

                    LOGGER.debug("({}/SERVER) Disconnected", address);

                    proxy.proxy.remove(proxy);
                }

                @Override
                boolean validate(String packet) {
                    return packet.getBytes(StandardCharsets.UTF_8).length < 1243568;
                }

                @Override
                public void onReceipt(String obj) {
                    proxy.client.write(EncodeUtils.decode(obj));
                }
            };

            new Thread(server).start();

            new Thread(client).start();

        } catch (IOException e) {

            proxy.remove(this);

            LOGGER.error(e);
            LOGGER.trace(e.getMessage(), e);
        }

    }

    public void disconnect() {
        if(server != null && !server.isClosed()) server.disconnect();
        if(client != null && !client.isClosed()) client.disconnect();

        proxy.remove(this);
    }

    public String getAddress() {
        return address;
    }
}
