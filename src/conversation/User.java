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
import javax.swing.JOptionPane;

import gui.*;
import serverside.ChatMeClient;
import serverside.ChatMeServer;


/* Recieve and send convos to server */
public class User {
	private int signal = 1;
	public  Message messagePackage;
	private BuddyList buddyList;
	private CreateAccount accountWindow;
	private ChatRoomGUI chatRoomGUI;
	public LogIn loginWindow;
	private JFrame chatWindow; 
	private String name;
	private String aboutme;
	private String password;
	private String imagePath;
	private Image image;
	private ArrayList<String> onlineUsers;
	private ArrayList <messageWindow> openConversations;
	private ArrayList<GroupConversation> currentConversations;	// change to Conversation 						
	private ChatMeClient chatClient;
	private ChatMeServer chatServer;
	private ArrayList<ChatRoomGUI> messageWindow;
	
	/* Constructor */
	public User() {
		createAccountWindow();
		onlineUsers = new ArrayList<String>();
		currentConversations = new ArrayList<GroupConversation>();
		messageWindow = new ArrayList<ChatRoomGUI>();
		//openConversations = new ArrayList<messageWindow>();
	}

	public void createAccountWindow() {
		accountWindow =  new CreateAccount(this);
	}

	public void createLoginWindow() {
		accountWindow.dispose();
		loginWindow = new LogIn(this);
	}

	public LogIn getLoginWindow()     {
		return loginWindow;
	}

	public void addClient(ChatMeClient client)	{
		this.chatClient = client;
		//System.out.println("adding client");
	}

	public void addGroupConvo(String convoName, String moderator)		{
		GroupConversation newConversation = new GroupConversation(convoName, this);
		currentConversations.add(newConversation);                                           
		buddyList.updateActiveConversations();
	}
	
	public void sendNewMessage(String content, String conversationName)		{															// send new message to server
		String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());				// find the time that message was created					 
		Message messageToSend = new Message(content,time,conversationName)															// create new message class to send server 
		chatServer.sendMessageAndObject(chatServer.NEW_MESSAGE_REQUEST, messageToSend);									
	}
	
	public void  receiveMessage (Message message)	{														// receive message from server and update GUI to display new message 
		for(messageWindow element : openConversations)	{													// only update GUI if open conversations exist
				// element.updateGUI(message.getContent(), message.getConversationName()); 				// fake code  
		}
	}

	public void removeGroupConvo(String convoName, String moderator)		{
		int i = 0;
		for(GroupConversation element : currentConversations)		{						// remove from currentConversations list 
			if(element.getName() == convoName)	{
				currentConversations.remove(i);
				buddyList.updateActiveConversations();
			}
			i++;
		}
	}

	public void displayConvoError() {						//called when addGroupConvo failed 
		//call gui for error message 
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
		JOptionPane.showMessageDialog(buddyList,"You entered incorrect information", "Message Dialog", JOptionPane.ERROR_MESSAGE);
	}

	public int getSignal() {
		return signal;
	}

	public void setSignal(int command)  {
		signal = command; 
	}

	public void setOnlineUsers(ArrayList<String> onlineUsers) {
		this.onlineUsers = onlineUsers;
	    this.onlineUsers.remove(this.getName());
	    buddyList.updateOnlineUser();
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
	
	public ArrayList<GroupConversation> getConversations() {
		return this.currentConversations;
	}
	
	public void setImagePath(String imagePath)		{
		this.imagePath = imagePath;
		image = new ImageIcon((imagePath)).getImage();
	}

	public String getImagePath()				{
		return this.imagePath;
	}

}
