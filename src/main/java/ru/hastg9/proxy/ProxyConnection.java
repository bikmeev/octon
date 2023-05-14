package ru.hastg9.proxy;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.hastg9.api.socket.IConnection;
import ru.hastg9.encode.HashUtils;
import ru.hastg9.packets.Packet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

abstract public class ProxyConnection implements IConnection {

    private final Logger LOGGER = LogManager.getLogger(Class.class);

    public static final int MAX_PACKET_SIZE = 932504;

    protected final Socket socket;
    protected ProxyClient proxy;

    protected PrintWriter writer;

    public ProxyConnection(Socket socket, ProxyClient proxy) {
        this.proxy = proxy;
        this.socket = socket;

        LOGGER.debug("({}) Incoming connection", getAddress());

    }

    @Override
    public void run() {
        try(InputStreamReader scanner = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)) {
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            int next;

            StringBuilder line = new StringBuilder();

            boolean lineBreaks = true;

            int size = 0;

            while ((next = scanner.read()) != -1) {

                if(next == 0xD || next == 0xA) {
                    if(lineBreaks) continue;

                    lineBreaks = true;

                    String str = line.toString();

                    line.delete(0, line.length());

                    size = 0;

                    if(!validate(str)) break;

                    if(str.equals(new String(new byte[]{0}))) {
                        write(str);
                        continue;
                    }

                    onReceipt(str);

                } else {
                    lineBreaks = false;

                    if(++size > MAX_PACKET_SIZE) break;

                    line.append((char) next);
                }

            }

/*            while (scanner.hasNextLine() && !socket.isClosed()) {

                System.out.println(scanner);

                String str = scanner.nextLine();

                if(!validate(str)) break;

                if(str.equals(new String(new byte[]{0}))) {
                    write(str);
                    continue;
                }

                onReceipt(str);

            }*/

            onClose();

        }catch (Exception ex) {
            LOGGER.debug("({}) Connection closed by server", getAddress());

            onClose();

            LOGGER.trace(ex.getMessage(), ex);
        }
    }

    public static boolean isJSONValid(String jsonInString) {
        try {
            Gson gson = new Gson();
            gson.fromJson(jsonInString, Packet.class);
            return true;
        } catch(com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }

    abstract void onClose();

    abstract boolean validate(String packet);

    @Override
    public void onSend(String packet) {
        LOGGER.debug("({}) <- {}", getAddress(), HashUtils.hashSum(packet));
    }

    @Override
    public void write(String packet) {
        onSend(packet);

        writer.println(packet);
    }

    @Override
    public int getID() {
        return 0;
    }

    @Override
    public Socket getSocket() {
        return socket;
    }

    @Override
    public String getAddress() {
        return proxy.getAddress();
    }

    public void disconnect() {
        try {
            socket.shutdownInput();
            socket.shutdownOutput();
            socket.close();
        } catch (IOException ignored) {

        }
    }

    @Override
    public boolean isClosed() {
        return socket.isClosed();
    }
}
