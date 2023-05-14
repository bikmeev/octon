package ru.hastg9.socket;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.hastg9.OctonMSG;
import ru.hastg9.api.socket.IConnection;
import ru.hastg9.encode.EncodeUtils;
import ru.hastg9.messenger.Group;
import ru.hastg9.messenger.User;
import ru.hastg9.messenger.UserManager;
import ru.hastg9.packets.Packet;
import ru.hastg9.packets.Packets;
import ru.hastg9.encode.HashUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class AnonConnection implements IConnection {

    private final Logger LOGGER = LogManager.getLogger(Class.class);

    protected final Socket socket;
    protected final int ID;
    protected AnonServer server;

    protected PrintWriter writer;

    public AnonConnection(Socket socket, AnonServer server) {
        this.server = server;
        this.socket = socket;
        ID = server.getLast() + 1;

        LOGGER.info("({}/{}) Incoming connection", getAddress(), ID);

    }

    @Override
    public void run() {
        try(Scanner scanner = new Scanner(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
//            new Thread(new Ping(this)).start();

            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            while (scanner.hasNextLine() && !socket.isClosed())
                onReceipt(EncodeUtils.decode(scanner.nextLine()));

/*            while ((obj = in.readObject()) != null && obj instanceof String) {

                String str = (String) obj;

                onReceipt(obj);

                write(str);

//                    Console.info(getClass(), "({}/{}) -> bytes={}", socket.getInetAddress().getHostAddress(), ID, );
            }*/
            LOGGER.info("({}/{}) Disconnected", getAddress(), ID);

            onClose();
        }catch (Exception ex) {
            LOGGER.info("({}/{}) Connection closed by server", getAddress(), ID);

            onClose();

            LOGGER.trace(ex.getMessage(), ex);
        }
    }

    public void onReceipt(Packet packet) {
        if(packet.getID() == Packets.AUTH) {
            if(packet.getSenderName().length() > 16) {
                sendAuth(false, null, "The maximum nickname length is 16 characters.");
                return;
            }

            String value = packet.getValue();

            if(value.length() != 32) {
                sendAuth(false, null,"The group token must be 24 characters long.");
                return;
            }

            UserManager manager = OctonMSG.getManager();

            if(manager.isOnline(packet.getSender())) {
                sendAuth(false, null,"User is already online.");
                return;
            }

            User user = manager.getMember(packet.getSender());

            Group group = manager.getGroupOrCreate(value);

            user.setConnection(this);
            user.setGroup(group.getToken());
            if(packet.getSenderName() != null) user.setName(packet.getSenderName());

            if(!group.contains(user)) manager.addMember(user);

            sendAuth(true, group.getToken());

            group.sendJoinOrLeave(user, true);

            if(!manager.getGroups().contains(group)) {
                manager.addGroup(group);

                user.sendMessage("The group does not exist. Creating a new group . . .");
                user.sendMessage("Group token: " + group.getToken());
            }

        }
        else if (packet.getID() == Packets.MESSAGE) {
            UserManager manager = OctonMSG.getManager();

            User user = manager.getMember(packet.getSender());
            String groupToken = user.getGroup();

            if(groupToken != null) {
                Group group = manager.getGroupOrCreate(groupToken);
                group.sendMessage(user, packet);
                return;
            }

        }
    }

    public void sendAuth(boolean successfully, String token) {
        sendPacket(new Packet(successfully ? token : null, null, Packets.AUTH));
    }

    public void sendAuth(boolean successfully, String token, String cause) {
        sendAuth(successfully, token);

        sendPacket(new Packet(cause, null, Packets.AUTH_MESSAGE));
    }

    @Override
    public void onReceipt(String packet) {
        LOGGER.debug("({}) -> {}", ID, HashUtils.hashSum(packet));

        Gson gson = new Gson();

        onReceipt(gson.fromJson(packet, Packet.class));
    }

    @Override
    public void onSend(String packet) {
        LOGGER.debug("({}) <- {}", ID, HashUtils.hashSum(packet));
    }

    @Override
    public void write(String packet) {
        onSend(packet);

        writer.println(EncodeUtils.encode(packet));
    }

    @Override
    public int getID() {
        return ID;
    }

    @Override
    public Socket getSocket() {
        return socket;
    }

    @Override
    public String getAddress() {
        return socket.getInetAddress().getHostAddress();
    }

    @Override
    public boolean isClosed() {
        return socket.isClosed();
    }

    public void onClose() {
        server.remove(this);

        OctonMSG.getManager().getMembers().forEach(user -> {
            if(user.getConnection() == this) {
                user.setConnection(null);

                String groupToken = user.getGroup();
                if(groupToken == null) return;

                Group group = OctonMSG.getManager().getGroup(groupToken);
                if(group == null) return;

                group.sendJoinOrLeave(user, false);
            }
        });

    }

    public void sendPacket(Packet packet) {

        packet.setHashSum(HashUtils.hashSum(packet.getValue()));

        Gson gson = new Gson();
        String encoded = gson.toJson(packet);

        write(encoded);
    }


    public void disconnect() {
        try {
            socket.shutdownInput();
            socket.shutdownOutput();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        onClose();
    }
}
