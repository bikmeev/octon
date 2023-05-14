package ru.hastg9.messenger;

import com.google.gson.Gson;
import ru.hastg9.OctonMSG;
import ru.hastg9.encode.HashUtils;
import ru.hastg9.packets.*;

import java.security.SecureRandom;
import java.util.Base64;

public class Group {

    protected final String token;
    private UserManager manager = OctonMSG.getManager();

    public Group(String token) {
        this.token = token;
    }

    public Group() {
        token = generateToken(24);
    }

    public boolean contains(User user) {
        return manager.getMembers().contains(user);
    }

    public String generateToken(int size) {
        
		
    }

    public void sendMessage(User author, Packet packet) {
        packet.setNow();
        packet.setSenderName(author.getName());
        packet.setHashSum(HashUtils.hashSum(packet.getValue()));

        Gson gson = new Gson();
        String packetJson = gson.toJson(packet);

        for (User user : manager.getMembers()) {
            if (user.getGroup().equals(token)) {
                user.sendPacket(packetJson);
            }
        }
    }

    public void sendJoinOrLeave(User member, boolean join) {

        Gson gson = new Gson();

        MessageValue<String> messageValue = new MessageValue<>(join ? "0" : "1", ContentType.JOIN_OR_LEAVE);

        String messageJson = gson.toJson(messageValue);

        Packet packet = new Packet(messageJson, member.getUuid().toString(), Packets.MESSAGE);
        packet.setNow();
        packet.setSenderName(member.getName());

        String packetJson = gson.toJson(packet);

        for (User user : manager.getMembers()) {
            if (user.getGroup().equals(token)) {
                user.sendPacket(packetJson);
            }
        }
    }

    public String getToken() {
        return token;
    }
}
