package ru.hastg9.messenger;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.hastg9.api.socket.IConnection;
import ru.hastg9.packets.*;
import ru.hastg9.encode.HashUtils;

import java.util.UUID;

public class User {

    protected final UUID uuid;
    protected String name;
    protected String group;

    private final Logger LOGGER = LogManager.getLogger(getClass());

    protected IConnection connection;

    public User(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public void setConnection(IConnection connection) {
        this.connection = connection;
    }

    public IConnection getConnection() {
        return connection;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void sendPacket(String packet) {
        if(!isOnline()) return;

        connection.write(packet);
    }

    public void sendMessage(String str) {

        if(!isOnline()) return;

        Gson gson = new Gson();

        MessageValue<String> messageValue = new MessageValue<>(str, ContentType.TEXT);

        String value = gson.toJson(messageValue);

        Packet message = Packets.createSystemMessage(value);

        message.setHashSum(HashUtils.hashSum(message.getValue()));

        String packet = gson.toJson(PacketUtils.encodePacket(message, group));

        connection.write(packet);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String groupToken) {
        this.group = groupToken;
    }

    public boolean isOnline() {
        if(connection == null) return false;
        return !connection.isClosed();
    }
}
