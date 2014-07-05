package conversation;

import gui.CreateAccount;
import gui.LogIn;

import java.awt.Image;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import gui.*;

import serverside.ChatMeClient;
import serverside.ChatMeServer;


/* Recieve and send convos to server */
public class User {
	private int signal = 1;
	public  Message messagePackage;
	private BuddyList buddyList;
	private CreateAccount accountWindow;
	private LogIn loginWindow;
	private JFrame chatWindow; 
	private String name;
	private String aboutme;
	private String password;
	private String status;
	private String imagePath;
	private Image image;
	private ArrayList<String> onlineUsers;
	private ArrayList<GroupConversation> currentConversations;		// change to Conversation 						
	private ChatMeClient chatClient;
	/* Constructor */
	public User() {
		createAccountWindow();
	}

	public void createAccountWindow() {
		accountWindow =  new CreateAccount(this);
	}
	
	public void createLoginWindow() {
		accountWindow.dispose();
		loginWindow = new LogIn(this);
	}
	
	public LogIn getLoginWindow(){
		return loginWindow;
	}
	
	public void addClient(ChatMeClient client){
		this.chatClient = client;
		System.out.println("adding client");
	}

	public void createNewAccount() {
		this.chatClient.sendCommand(ChatMeServer.NEW_USER_REQUEST);
		// verify if isn't taken, wait for respose // 
		
		createLoginWindow();
	}
	
	public void sendLogInRequest() {	
		chatClient.sendCommand(ChatMeServer.LOGIN_REQUEST);
		//update GUI based on online users 
		//UpdateBuddyList(this);
	}
	
	public void createBuddyList() {
		buddyList = new BuddyList(this);
		loginWindow.dispose();
	}
	
	public void nameExistError() {
		accountWindow.displayError();
	}
	
	public void incorrectInfoError() {
		
	}
	
	public int getSignal() {
		return signal;
	}

	public void setSignal(int command)  {
		signal = command; 
	}

	public void startNewConversation(String conversationName) {					//single conversation
		//chatWindow.beginConversation(conversationName);
	}

	/* send message to the users in conversation */
	public void sendMessage(String message, GroupConversation convo)	{
		String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());							 
		//new Message(message,time,conversationName);							// create new message class 
		//TODO: send message class to server 
	}

	public void setOnlineUsers(ArrayList<String> onlineUsers) {
		this.onlineUsers = onlineUsers;
	}
	
	public ArrayList<String> getOnlineUsers(){
		return this.onlineUsers;
	}
	
	public BuddyList getBuddyList() {
		return this.buddyList;
	}
	public void setName(String name)	{
		this.name = name;
	}

	public String getName()		{
		return this.name;
	}

	public void setAboutme(String aboutme)	{
		this.aboutme = aboutme;
	}

	public String getAboutme()				{
		return this.aboutme;
	}
	public void setPassword(String password)	{
		this.password = password;
	}

	public String getPassword() 		{
		return this.password;
	}

	public void setImagePath(String imagePath)		{
		this.imagePath = imagePath;
		image = new ImageIcon(this.getClass().getResource(imagePath)).getImage();
	}

	public String getImagePath()				{
		return this.imagePath;
	}
	
}
