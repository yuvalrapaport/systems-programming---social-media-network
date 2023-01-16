package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {

    private static ConnectionsImpl connections;
    private ConcurrentHashMap<Integer, ConnectionHandler> clientsId;
    private ConcurrentHashMap<String, User> registered;
    private ConcurrentHashMap<Integer,User> clientToUser; //connection handler ID to User


    private ConnectionsImpl(){
        clientsId = new ConcurrentHashMap<>();

        registered = new ConcurrentHashMap<>();
        clientToUser = new ConcurrentHashMap<>();
    }

    public static synchronized ConnectionsImpl getInstance(){
        if (connections == null)
            connections = new ConnectionsImpl<>();
        return connections;
    }

    public synchronized boolean send(int connectionId, T msg){
        ConnectionHandler CH = clientsId.get(connectionId);
        if (CH != null) {
            CH.send(msg);
            return true;
        }
        return false;
    }

    public void broadcast(T msg){
        for (User user : getLoggedInUsers()){
            if (user.isLoggedIn()){
                clientsId.get(user.getConnId()).send(msg);
            }
        }
    }

    public synchronized void disconnect(int connectionId){
        User user = clientToUser.get(connectionId);
        user.setStatus(false);
        user.setConnId(-1);
        clientToUser.remove(connectionId);
        clientsId.remove(connectionId);
    }

    public synchronized boolean isRegistered(String username){
        if (registered.get(username) == null)
            return false;
        return true;
    }

    public synchronized void addToRegistered(Integer connId, String[] split){
        User user = new User(split[0], split[1], split[2]);
        registered.put(split[0], user);
    }

    public synchronized void logIn(User user, int connId){
        user.logIn(connId);
        clientToUser.put(connId,user);
    }

    public synchronized boolean checkPassword (String username, String password){
        return (registered.get(username)).getPassword().equals(password);
    }

    public synchronized boolean isLoggedIn (User user){
        if (user == null)
            return false;
        return user.isLoggedIn();
    }


    public synchronized User getUser (int connId){
        return clientToUser.get(connId);
    }

    public synchronized User getUserByName (String username){
        return registered.get(username);
    }


    public synchronized void startFollowing(User user, String target){
       User targetUser = registered.get(target);
       user.addFollowing(targetUser);
       targetUser.addFollower(user);
    }

    public synchronized void stopFollowing(User user, String target){
        User targetUser = registered.get(target);
        user.removeFollowing(targetUser);
        targetUser.removeFollower(user);
    }


    public synchronized void addConnectionHandler(Integer connId, ConnectionHandler CH){
        clientsId.put(connId,CH);
    }

    public synchronized List<User> getLoggedInUsers(){
        List<User> logged = new LinkedList<>();
        for (String username : registered.keySet()){
            if (registered.get(username).isLoggedIn())
                logged.add(registered.get(username));
        }
        return logged;
    }
}
