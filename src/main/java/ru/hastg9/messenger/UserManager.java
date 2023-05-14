package ru.hastg9.messenger;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

final public class UserManager {

    private final CopyOnWriteArrayList<Group> groups = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<User> members = new CopyOnWriteArrayList<>();

    public void addGroup(Group group) {
        groups.add(group);
    }

    public void addMember(User member) {
        members.add(member);
    }

    public User getMember(String uuid) {
        return getMember(UUID.fromString(uuid));
    }

    public boolean isOnline(UUID uuid) {
        User member = members.stream().filter(user -> user.uuid.equals(uuid)).findFirst().orElse(null);

        if(member == null) return false;

        return member.isOnline();
    }

    public void removeMember(User user) {
        members.remove(user);
    }

    public User getMember(UUID uuid) {
        return members.stream().filter(user -> user.uuid.equals(uuid)).findFirst().orElse(new User(uuid, "Anonymous"));
    }

    public void removeGroup(Group group) {
        groups.remove(group);
    }

    public Group getGroupOrCreate(String token) {
        return groups.stream().filter(group -> group.token.equals(token)).findFirst().orElse(new Group());
    }

    public Group getGroup(String token) {
        return groups.stream().filter(group -> group.token.equals(token)).findFirst().orElse(null);
    }

    public CopyOnWriteArrayList<Group> getGroups() {
        return groups;
    }

    public CopyOnWriteArrayList<User> getMembers() {
        return members;
    }
}
