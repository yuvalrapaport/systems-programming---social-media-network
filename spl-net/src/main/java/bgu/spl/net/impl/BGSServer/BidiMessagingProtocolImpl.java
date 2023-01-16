package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.impl.BGSServer.ConnectionsImpl;
import bgu.spl.net.impl.BGSServer.User;
import bgu.spl.net.srv.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

import java.time.chrono.MinguoDate;
import java.util.List;

public class BidiMessagingProtocolImpl implements BidiMessagingProtocol<String> {

    private boolean shouldTerminate;
    private int connId;
    private ConnectionsImpl connections;

    public BidiMessagingProtocolImpl(){}

    public void start(int connectionId, Connections<String> connections){
        this.connId = connectionId;
        this.connections = ConnectionsImpl.getInstance();
        shouldTerminate = false;
    }
    
    public void process(String message){
        String op = message.substring(0,message.indexOf(" "));
        String substring ="";
        if (message.charAt(message.length()-1) == ' ' && !op.equals("3") && !op.equals("7"))
            substring = message.substring(message.indexOf(" ") + 1, message.length() - 1);
        if (op.equals("1")){ //REGISTER
            register(substring);
        }

        if (op.equals("2")){ //LOGIN
            login(message.substring(message.indexOf(" ")+1));
        }

        if (op.equals("3")){ //LOGOUT
           logout();
        }

        if (op.equals("4")){ //FOLLOW
            if (substring.charAt(0) == '1'){
                unfollow(substring.substring(2)); //target username
            }
            else
                follow(substring.substring(2));
        }

        if (op.equals("5")){ //POST
           post(substring);
        }


        if (op.equals("6")){//PM
            PM(substring);
        }

        if (op.equals("7")){ //LOGSTAT
            logstat();
        }

        if(op.equals("8")){//STAT
            stat(substring);
        }

        if (op.equals("12")){//BLOCK
            block(substring);
        }

    }

    public boolean shouldTerminate(){
        return shouldTerminate;
    }

    private void register(String msg){
        String[] split = msg.split(" ");
        if (!connections.isRegistered(split[0])){
            connections.addToRegistered(connId, split);
            String output = "10 1 ";
            connections.send(connId,output);
        }
        else{
            String output = "11 1 ";
            connections.send(connId,output);
        }
    }

    private void login(String msg){
        String[] split = msg.split(" ");
        if (!connections.isRegistered(split[0]) || !connections.checkPassword(split[0],split[1]) || connections.isLoggedIn(connections.getUser(connId)) || split[2].equals("0")){
            String output = "11 2 ";
            connections.send(connId,output);
        }
        else if (split[2].equals("1")){
            User user = connections.getUserByName(split[0]);
            connections.logIn(user, connId);
            String output = "10 2 ";
            connections.send(connId,output);
            while (!user.getNotifications().isEmpty()){
                String temp = user.getNotifications().poll();
                connections.send(connId,temp);
            }
        }
    }

    private void logout(){
        if (!connections.isLoggedIn(connections.getUser(connId))){
            String output = "11 3 ";;
            connections.send(connId,output);
        }
        else{
            String output = "10 3 ";
            connections.send(connId,output);
            connections.disconnect(connId);
            shouldTerminate = true;
        }
    }

    private void unfollow(String target){
        if (connections.isRegistered(target)
                && connections.isLoggedIn(connections.getUser(connId))
                && connections.getUser(connId).getFollowing().contains(connections.getUserByName(target))) {
            connections.stopFollowing(connections.getUser(connId), target);
            String output = "10 4 "+target;
            connections.send(connId,output);
        }
        else{
            String output = "11 4 ";
            connections.send(connId,output);
        }
    }

    private void follow(String target){
        User targetUser = connections.getUserByName(target);
        User user = connections.getUser(connId);
      
        if (connections.isRegistered(target)
                && connections.isLoggedIn(user)
                && !user.getFollowing().contains(targetUser)
                && !user.getBlocked().contains(targetUser)
                && !targetUser.getBlocked().contains(user)){
            connections.startFollowing(connections.getUser(connId), target);
            String output = "10 4 "+target;
            connections.send(connId,output);
        }
        else{
            String output = "11 4 ";
            connections.send(connId,output);
        }

    }
    private void post(String msg){
        if (connections.isLoggedIn(connections.getUser(connId))){
            User user = connections.getUser(connId);
            user.incrementNumOfPosts();
            String temp = "9 1 "+user.getName()+" "+msg;
            for (User follower : user.getFollowers()){
                if (follower.isLoggedIn()){
                    connections.send(follower.getConnId(),temp);}
                else{
                    follower.addNotification(temp);
                }
            }
            String str ="";
            boolean flag = false;
            for (int i= 0 ; i<msg.length(); i++){
                if (msg.charAt(i) == '@'){
                    flag = true;
                    continue;
                }
                if (flag)
                    str += msg.charAt(i);
                if ((msg.charAt(i) == ' ' || i == msg.length() -1) && flag){
                    if (msg.charAt(i) == ' ')
                    	str = str.substring(0,str.length()-1);
                    flag = false;
                    User target = connections.getUserByName(str);
                    if (target != null  && !user.getFollowers().contains(target) && !user.getBlocked().contains(target) && !target.getBlocked().contains(user)){
                        if (target.isLoggedIn())
                            connections.send(target.getConnId(), temp);
                        else
                            target.addNotification(temp);
                        str = "";
                    }
                }
            }
            String output = "10 5 ";
            connections.send(connId,output);
        }
        else{
            String output = "11 5 ";
            connections.send(connId,output);
        }
    }

    private void PM(String msg) {
        String username = msg.substring(0, msg.indexOf(" "));
        String content = msg.substring(msg.indexOf(" ")+1);
        User user = connections.getUser(connId);
        User target = connections.getUserByName(username);
        if (connections.isLoggedIn(user) && !target.getBlocked().contains(user) && !user.getBlocked().contains(target)) {
            content = filter(content);
            String temp = "9 0 "+user.getName()+" "+ content;
            if (target.isLoggedIn())
                connections.send(target.getConnId(), temp);
            else
                target.addNotification(temp);

            String output = "10 6 ";
            connections.send(connId, output);
        }
        else{
            String output = "11 6 ";
            connections.send(connId,output);
        }
    }

    public void logstat (){
        User sender = connections.getUser(connId);
        if (connections.isLoggedIn(sender)){
           List<User> logged = connections.getLoggedInUsers();
           for (User user : logged){
               if (!user.getBlocked().contains(sender) && !sender.getBlocked().contains(user)) {
                   String temp = "10 7 "+user.getAge()+" "+user.getNumOfPosts()+" "+user.getFollowers().size()+" "+user.getFollowing().size();
                   connections.send(connId, temp);
               }
           }
        }
        else {
            String output = "11 7 ";
            connections.send(connId,output);
        }
    }

    public void stat(String msg){
        if (connections.isLoggedIn(connections.getUser(connId))){
            String[] split = msg.split("\\|");
            for (String username : split){
                User user = connections.getUserByName(username);
                if (user != null && !user.getBlocked().contains(connections.getUser(connId)) && !connections.getUser(connId).getBlocked().contains(user)) {
                    String temp = "10 8 "+user.getAge()+" "+user.getNumOfPosts()+" "+user.getFollowers().size()+" "+user.getFollowing().size();
                    connections.send(connId, temp);
                }
                else {
                    String output = "11 8 ";
                    connections.send(connId,output);
                }
            }
        }
        else {
            String output = "11 8 ";
            connections.send(connId,output);
        }
    }

    public void block(String username){
        User sender = connections.getUser(connId);
        User target = connections.getUserByName(username);
        if (sender != null && sender.isLoggedIn() && !sender.getBlocked().contains(target) && target!= null){
            sender.block(target);
            if (target.getFollowing().contains(sender)) {
                target.removeFollowing(sender);
                sender.removeFollower(target);
            }
            if (sender.getFollowing().contains(target)){
                sender.removeFollowing(target);
                target.removeFollower(sender);
            }
            String output ="10 12 ";
            connections.send(connId,output);
        }

        else{
            String output = "11 12 ";
            connections.send(connId,output);
        }

    }

    private String filter (String content){ //TODO this
        content = " " + content + " ";
        final String[] toFilter = {"war", "trump"};
        for (String toChange : toFilter) {
            content = content.replaceAll(' ' + toChange + ", ", " <filtered> ");
            content = content.replaceAll(' ' + toChange + ". ", " <filtered> ");
            content = content.replaceAll(' ' + toChange + "? ", " <filtered> ");
            content = content.replaceAll(' ' + toChange + "! ", " <filtered> ");
            content = content.replaceAll(' ' + toChange + " ", " <filtered> ");
            content = content.replaceAll(' ' + toChange + "\r", " <filtered> ");
        }

        if(content.charAt(0) == ' ')
            content = content.substring(1);
        if(content.charAt(content.length()-1) == ' ')
            content = content.substring(0,content.length()-1);

        return content;
    }
}
