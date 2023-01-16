package bgu.spl.net.impl.BGSServer;

import java.util.*;

public class User {
    private String name;
    private boolean status;
    private String password;
    private String birthday;
    private List<User> followers;
    private List<User> following;
    private List<User> IBlocked;
    private Queue<String> notifications;
    private Integer currConnId = -1;
    private int numOfPosts = 0;

    public User(String name, String password, String birthday){
        this.name = name;
        this.status = false;
        this.password = password;
        this.birthday = birthday;
        this.followers = new LinkedList<>();
        this.following = new LinkedList<>();
        this.IBlocked = new LinkedList<>();
        this.notifications = new LinkedList<>();
    }

    public boolean isLoggedIn() {
        return status;
    }

    public List<User> getFollowing() {
        return following;
    }

    public List<User> getFollowers() {
        return followers;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public List<User> getBlocked() {
        return IBlocked;
    }

    public void setStatus(boolean status){
        this.status = status;
    }

    public void addFollower (User user){
        followers.add(user);
    }

    public void addFollowing (User user){ following.add(user); }

    public void removeFollower(User user){
        followers.remove(user);
    }

    public void removeFollowing(User user){ following.remove(user); }

    public void block (User user){
        IBlocked.add(user);
    }

    public void addNotification(String msg){
        notifications.add(msg);
    }

    public Queue<String> getNotifications() {
        return notifications;
    }

    public void setConnId(Integer currConnId) {
        this.currConnId = currConnId;
    }

    public Integer getConnId() {
        return currConnId;
    }

    public void incrementNumOfPosts(){
        numOfPosts++;
    }

    public int getNumOfPosts() {
        return numOfPosts;
    }

    public int getAge(){
        int currYear = Calendar.getInstance().get(Calendar.YEAR);
        int currMonth = Calendar.getInstance().get(Calendar.MONTH);
        int currDay = Calendar.getInstance().get(Calendar.DATE);
        String[] split = birthday.split("-");
        int bdYear = Integer.parseInt(split[2]);
        int bdMonth = Integer.parseInt(split[1]);
        int bdDay = Integer.parseInt(split[0]);
        if (bdMonth>= currMonth && bdDay>currDay)
            return currYear-bdYear-1;
        return currYear-bdYear;

    }

    public void logIn (Integer connId){
        this.setStatus(true);
        this.setConnId(connId);
    }
}
