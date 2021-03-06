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
	private MessageWindow groupConversationWindow;
	public LogIn loginWindow;
	private JFrame chatWindow; 
	private String name;
	private String bio;
	private String password;
	private String imagePath;
	private Image image;
	private ArrayList<String> onlineUsers;
	private ArrayList <MessageWindow> openConversations;
	private ArrayList<String> currentConversations;					//buddylist									
	private ChatMeClient chatClient;
	
	/* Constructor */
	public User() {
		createAccountWindow();
		onlineUsers = new ArrayList<String>();
		currentConversations = new ArrayList<String>();
		//messageWindow = new ArrayList<MessageWindow>();
		openConversations = new ArrayList<MessageWindow>();
		
	}

	public void createAccountWindow() {
		accountWindow =  new CreateAccount(this);
	}
	public void displayClearMessage() {
		
	}
	public void createLoginWindow() {
		accountWindow.dispose();
		loginWindow = new LogIn(this);
	}

//	public void createGroupConversationWindow(String convoName) {
		//groupConversationWindow = new MessageWindow(convoName,this,MessageWindow.GROUP_CHAT);
//	}

	public LogIn getLoginWindow()     {
		return loginWindow;
	}

	public void addClient(ChatMeClient client)	{
		this.chatClient = client;
		//System.out.println("adding client");
	}
	public void setCurrentConversations(ArrayList<String> convos) {
		ArrayList<String> temp = new ArrayList<String>();
		for(int i = 0; i < convos.size(); i++)	{
			if(!convos.get(i).contains("@"))	{
				temp.add(convos.get(i));
			}
		}
		currentConversations = temp;
		buddyList.updateActiveConversations();
	}
	public void initiateGroupConvoRequest(String convoName){
		//System.out.println("IN initiateGroupConvoRequest. Convo is : " + convoName);
		chatClient.sendCommand(ChatMeServer.NEW_GROUP_REQUEST, convoName);
	}
	
	public void initiatePrivateConvoRequest(String convoName){
		//System.out.println("IN initiatePrivateConvoRequest. Convo is : " + convoName);
		chatClient.sendCommand(ChatMeServer.NEW_PRIVATE_REQUEST, convoName);
	}
	
	public void addToOpenConversations(MessageWindow mw)	{
		openConversations.add(mw);
	}
	
	public void removeGroupConvoRequest(String convoName){
		chatClient.sendCommand(ChatMeServer.END_GROUP_REQUEST, convoName);
		//System.out.println("remove request entered");
		
	}
	public void closeMessageWindow(String convoName){
		System.out.println("size of open conversation is " + openConversations.size());
		for(MessageWindow element : openConversations)	{
			System.out.println("Name of the message window is " + element.getName());
			if(element.getName().equals(convoName))	{
				openConversations.remove(element);
				break;
			}
		}
		System.out.println("new size of open convo is " + openConversations.size());
	}
	
	public void sendNewGroupMessage(String content, String conversationName)		{															// send new message to server
		String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());				// find the time that message was created					 
		content = this.getName() + ": " + content;								
		Message messageToSend = new Message(content,time,conversationName);														// create new message class to send server 
		chatClient.sendCommand(ChatMeServer.NEW_GROUP_MESSAGE_REQUEST, messageToSend);									
	}
	public void sendNewPrivateMessage(String content, String conversationName)		{
		String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());				// find the time that message was created					 
		content = this.getName() + ": " + content;								
		Message messageToSend = new Message(content,time,conversationName);														// create new message class to send server 
		chatClient.sendCommand(ChatMeServer.NEW_PRIVATE_MESSAGE_REQUEST, messageToSend);									
	}	
	public void getGroupMessage (Message msg)		{
//		System.out.println("CLIENT: (CHECKING OPEN CONVERSATIONS) Conversation name: " + msg.getConversationName());
		for(MessageWindow element : openConversations)	{	// only update GUI if open conversations exist
//			System.out.println("convo name: " + element.getName());
			if(element.getName().equals(msg.getConversationName())) {
				element.updateContent(msg.getContent()); 				  
			}
		}
	}
	public void getPrivateMessage(Message msg)	{
		//System.out.println("Client: Conversation name : " + msg.getConversationName());
		for(MessageWindow element : openConversations)	{	// only update GUI if open conversations exist
			//System.out.println("convo name: " + element.getName());
			System.out.println("name of the message window is " + element.getName());
			if(element.getName().equals(msg.getConversationName())) {
				element.updateContent(msg.getContent()); 	
				return;
			}
		}
//		System.out.println("$$$$$$$$$$$$$conversation window was not openeddd$$$$$$$$$$");
		MessageWindow mw = new MessageWindow(msg.getConversationName(),this);
		String [] name = msg.getConversationName().split("@");
		for(int i = 0; i < name.length; i++)	{
			if(!name[i].isEmpty() && !name[i].equals(this.getName())) {
				mw.setTitle(name[i]);
				mw.updateContent(msg.getContent());
				addToOpenConversations(mw);
				return;
			}
		}		
		System.err.println("should have returned by now....error");
	}
	
	public boolean windowIsOpen(String name)		{
		System.out.println(" Inside window is open");
		for(MessageWindow element : openConversations )	{
			System.out.println("message window name is " + element.getName() );
			if(element.getName().equals(name))	{
				return true;
			}
		}
		 return false; 
	}
	public void addToOnlineConversations(MessageWindow mw)	{
		System.out.println("$$$$$$$$Inside addtoOnlineConversations$$$$$$");
		for(MessageWindow element : openConversations)	{
			System.out.println("message window name is " + element.getName());
		}
		System.out.println("For loop done. Adding message window " + mw.getName());
		openConversations.add(mw);
		System.out.println("new open conversations size is " + openConversations.size());

		// set conditions to check if it is private 
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
		this.chatClient.sendCommand(ChatMeServer.LOGIN_REQUEST);
		//update GUI based on online users 
		//UpdateBuddyList(this);
	}
	public void createBuddyList() {
		buddyList = new BuddyList(this);
		loginWindow.dispose();
	}
	public void getBioRequest(String name){
		chatClient.sendCommand(ChatMeServer.GET_BIO_REQUEST, name);
	}
	public void setBioRequest(String name, String newBio){
		Message msg = new Message(name, newBio);
		chatClient.sendCommand(ChatMeServer.SET_BIO_REQUEST, msg);
	}
	
	public void displayFriendBio(String username, String imagePath, String bio){
		buddyList.aboutMeAction(username, imagePath, bio);
	}
	public void nameExistError() {
		accountWindow.displayError();
	}
	public void incorrectInfoError() {
		JOptionPane.showMessageDialog(buddyList,"You entered incorrect information", "Message Dialog", JOptionPane.ERROR_MESSAGE);
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

	public void setBio(String bio)	{
		this.bio = bio;
	}

	public String getBio()				{
		return this.bio;
	}
	public void setPassword(String password)	{
		this.password = password;
	}

	public String getPassword() {
		return this.password;
	}
	
	public ArrayList<String> getConversations() {
		return this.currentConversations;
	}
	
	public void setImagePath(String imagePath)		{
		this.imagePath = imagePath;
		image = new ImageIcon((imagePath)).getImage();
	}

	public String getImagePath()				{
		return this.imagePath;
	}
	
	public void signOut()   {
		this.chatClient.sendCommand(ChatMeServer.SIGN_OUT_REQUEST);
	}
	
	public void createNewMessageWindow(String conversationName, boolean moderator)	{
		MessageWindow mw = new MessageWindow(conversationName,this);
		//System.out.println("Entered createNewMessageWindow");
		if(moderator == true )	{									// set moderator 
			//System.out.println("moderator is true");
			mw.setModerator();
		}
		else {
			//System.out.println("not moderator!");
		}
		addToOnlineConversations(mw);
	}
	
	public void createNewMessageWindow(String conversationName, boolean moderator, String msg)	{
		MessageWindow mw = new MessageWindow(conversationName,this);
		//System.out.println("Entered createNewMessageWindow");
		if(moderator == true )	{									// set moderator 
			//System.out.println("moderator is true");
			mw.setModerator();
		}
		else {
			//System.out.println("not moderator!");
		}
		mw.updateContent(msg);
		addToOnlineConversations(mw);
	}
}
