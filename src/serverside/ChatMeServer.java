package serverside;
import java.awt.Image;
import java.io.*;
import java.net.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.Icon;

import conversation.Message;
import database.Database;
public class ChatMeServer {
	
	public static int NEW_USER_REQUEST = 0;
	public static int LOGIN_REQUEST = 1;
	public static int SIGN_OUT_REQUEST = 2;
	public static int UPDATE_ONLINE_USERS_REQUEST = 3;
	public static int NEW_GROUP_REQUEST = 4;
	public static int END_GROUP_REQUEST = 5;
	public static int UPDATE_GROUP_REQUEST = 388;
	public static int NEW_GROUP_MESSAGE_REQUEST = 6;
	public static int NEW_PRIVATE_MESSAGE_REQUEST = 7;


	Database database;
	ArrayList<SocketHolder> clients;

	private Socket userReqSocket;
	private Socket servReqSocket;
	
	private Lock clientLock = new ReentrantLock();
	
	public ChatMeServer() throws IOException{
		printDbg("Starting server...");
		ServerSocket ss1 = new ServerSocket(7777);
		ServerSocket ss2 = new ServerSocket(8888);
		printDbg("Server started...");
		
		clients = new ArrayList<SocketHolder> ();
		database = new Database();
		
		while(true){
			userReqSocket = ss1.accept();
			servReqSocket = ss2.accept();
			
			printDbg("Connection from: " + userReqSocket.getInetAddress());
			
			ObjectOutputStream userOut = new ObjectOutputStream(userReqSocket.getOutputStream());
			ObjectInputStream  userIn = new  ObjectInputStream(userReqSocket.getInputStream());
			
			ObjectOutputStream servOut = new ObjectOutputStream(servReqSocket.getOutputStream());
			ObjectInputStream  servIn = new ObjectInputStream(servReqSocket.getInputStream());
			
		
			SocketHolder sh = new SocketHolder(userIn, userOut, servIn, servOut);
			printDbg(" ~ line 63 problem line.");

			clientLock.lock();
			clients.add(sh);
			clientLock.unlock();
			
			UserReqThread ct = new UserReqThread(userIn, userOut);
			ServReqThread srt = new ServReqThread(servIn, servOut);
			
			ct.setServReqThread(srt);
			srt.setUserReqThread(ct);
			
			ct.setSocketHolder(sh);
			srt.setSocketHolder(sh);
			
			ct.start();
			srt.start();
			

		}		
	}
	

	public static void printDbg(String message) {
		System.out.println(Thread.currentThread().toString() + message);
	}
	public static void main(String [] args) throws IOException{
		new ChatMeServer();
	}

	class UserReqThread extends Thread {
		
		private ObjectOutputStream threadUserOut;
		private ObjectInputStream threadUserIn;
		private ServReqThread srt;
		private SocketHolder sh;
		
		/* Constructor */
		public UserReqThread(ObjectInputStream in, ObjectOutputStream out) throws IOException{
			threadUserIn = in;
			threadUserOut = out;
		}
		//constructor helpers
		public void setSocketHolder(SocketHolder sh){
			this.sh = sh;
		}
		public void setServReqThread(ServReqThread srt) {
			this.srt = srt;
		}
		public void sendWelcomeMessage(){
			String message = "Welcome. Please Enter a Command.";
			try {
				printDbg("Welcome message Sent\n");
				threadUserOut.writeObject(message);
				
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		/* * * * * * * * * * * * * * * * * * *
		 * 	 Thread LISTEN for SERVER block  *
		 * * * * * * * * * * * * * * * * * * */
		public void run(){	
				
			//1. Send Welcome Message
			sendWelcomeMessage();
			
			//2. Listen for Signal
			printDbg("SERVER: Listening for command");
			while(true)
			{	
				try {
					int command = threadUserIn.readInt(); //Read in whatever command is sent from across socket
					handleCommand(command);
					
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
					threadUserOut = null;
					threadUserIn = null;
				}
			}
		}
		
		/* * * * * * * * * * * * * * * * * * *
		 * Thread DO action from USER block  *
		 * * * * * * * * * * * * * * * * * * */
		private void handleCommand(int command) throws IOException, ClassNotFoundException {
			
			printDbg("SERVER: parsing command...");
			try{
				if(command == NEW_USER_REQUEST){
					newUserRequest();
				}
				else if(command==LOGIN_REQUEST){
					loginRequest();
				}
				else if(command == SIGN_OUT_REQUEST){
					signOutRequest();
				}
				else if(command == NEW_GROUP_REQUEST){
					newGroupRequest();
				}
				else if(command == END_GROUP_REQUEST){
					endGroupRequest();
				}
				else if(command == NEW_GROUP_MESSAGE_REQUEST){
					newGroupMessageRequest();
				}
				else if(command == NEW_PRIVATE_MESSAGE_REQUEST){
					newPrivateMessageRequest();
				}
			} catch (SQLException s){
				s.printStackTrace();
			}
		}
		
		private void newUserRequest() throws IOException, ClassNotFoundException, SQLException{
			//SAT: FINISHED
			printDbg("SERVER: Command recieved on server: New User");
			String username = (String) threadUserIn.readObject();
			String password = (String) threadUserIn.readObject();
			String bio 	    = (String) threadUserIn.readObject();
			String imgPath 	= (String) threadUserIn.readObject();
			printDbg("SERVER READS:"
					+ username + " " + password + " " + bio + " " + imgPath);
			
			boolean OK = database.verifyUserExists(username); //database compatible
			
			threadUserOut.writeBoolean(OK);
			threadUserOut.flush();
			if(OK == false){
				database.createAccount(username, password, bio, imgPath);
			}
		}
		private void loginRequest() throws IOException, ClassNotFoundException, SQLException{

			printDbg("Command recieved on server: Login\n");
			String un = (String) threadUserIn.readObject();
			String pw = (String) threadUserIn.readObject();
			printDbg("Reading in: " + un);
			printDbg("Reading in: " + pw);

			boolean OK = database.login(un, pw);

			if(OK == true){
				
				database.addToOnlineList(un); //synchronized
				printDbg("Giving OK to log in.");
				printDbg("Attempting to send online Users");
				
				threadUserOut.writeBoolean(true);
				threadUserOut.flush(); //send OK
				
				String bio = database.getBio(un);
				threadUserOut.writeObject(bio);				
				String imgPath = database.getImagePath(un);
				threadUserOut.writeObject(imgPath);

				threadUserOut.flush();
				
				printDbg("Finished command");
				
				registerSocket(un); //<-- Synchronized server log in
				updateOnlineUsers();
				updateCurrentConversations();

			}
			else{
				printDbg("Denying user.");
				threadUserOut.writeBoolean(false);
				threadUserOut.flush();
				printDbg("Finished command");
			}
		}
		private void signOutRequest()throws IOException, ClassNotFoundException{
			//unfinished: client needs to recieve this information, AND client should just close shop once it sends the sign out request ... nothing to send back to client
			String name = (String) threadUserIn.readObject();
			printDbg("Sign out. Reading in: " + name);
			
			clientLock.lock(); //don't want people sending me messages as I log out
				database.signOut(name); //might have to work out nitty gritty details w/ Kelsey
				updateOnlineUsers();
				clients.remove(this.sh);
			clientLock.unlock();
			
			//By the time we get this message, client will have shut down.

		}
		
		private void newGroupRequest() throws ClassNotFoundException, IOException{
			//needs database implementation
			printDbg("Reading group convo initiation request");
			String convoName = (String) threadUserIn.readObject();
			boolean OK = true;
			threadUserOut.writeBoolean(OK); //Change this later when we want to put limitations on when a new group conversation can be created
			threadUserOut.flush();
			database.createConversation(convoName, "");
			updateCurrentConversations();
		}
		private void endGroupRequest() throws ClassNotFoundException, IOException {
			// unfinished ??? client might have to write code for this
			printDbg("Reading group convo deletion request");
			String convoName = (String) threadUserIn.readObject();
			String moderator = (String) threadUserIn.readObject();
			database.endConvo(convoName); 
			updateCurrentConversations();
		}
		
		private void updateOnlineUsers() throws IOException{
			clientLock.lock();
				ArrayList<String> strArr = database.getOnlineList();
			clientLock.unlock();
			
			srt.updateAllOnlineUsers(strArr);
		}
		private void updateCurrentConversations() throws IOException{
			clientLock.lock();
				ArrayList<String> strArr = database.getGroupConversations();
			clientLock.unlock();
			srt.updateAllConvos(strArr);
		}
		
		private void newGroupMessageRequest() throws ClassNotFoundException, IOException{
			Message msg = (Message) threadUserIn.readObject();
			srt.sendMessageToAll(msg);
		}
		private void newPrivateMessageRequest() throws ClassNotFoundException, IOException{
			Message msg = (Message) threadUserIn.readObject();
			srt.sendMessageToRecipients(msg);
		}
		
		/* call this method upon logging in  */
		public void registerSocket(String name){
			//synchronized client grabbing operation
			clientLock.lock();
				Iterator<SocketHolder> it = clients.iterator();
				printDbg("Clients has " + clients.size() + " elemnts in it...");
				while(it.hasNext()){
					if(this.sh == it.next()){
						sh.setName(name);
					}
				}
			clientLock.unlock();
		}
		/* * * * * * * * * * * * * *
		 * end USER request Thread *
		 * * * * * * * * * * * * * */
		
	}
	class ServReqThread extends Thread{
		
		ObjectOutputStream servOut;
		ObjectInputStream  servIn;
		private UserReqThread ct;
		private SocketHolder sh;
		
		public ServReqThread(ObjectInputStream servIn, ObjectOutputStream servOut){
			this.servOut = servOut;
			this.servIn  = servIn;
		}
		
		public void run(){
			//Listen for Commands from database
			while (true) {
				
			}
			
		}
		public void setSocketHolder(SocketHolder sh){
			this.sh = sh;
		}
		public void setUserReqThread(UserReqThread ct) {
			this.ct = ct;
		}
		
		/* * * * * * * * * * * * * * * * *
		 * 	Send Information to clients  *
		 * * * * * * * * * * * * * * * * */
		public void updateAllOnlineUsers(ArrayList<String> onlineUsers) throws IOException{
			printDbg("Online users: ");
			for(int i=0;i<onlineUsers.size();i++){
				printDbg("user: " + onlineUsers.get(i));
			}
			clientLock.lock();
				for(int i=0; i<clients.size();i++){
					if( ! clients.get(i).getName().isEmpty() ){
						ObjectOutputStream oos = clients.get(i).serverOut;
						oos.writeInt(UPDATE_ONLINE_USERS_REQUEST);
						oos.flush();
						oos.writeObject(onlineUsers);
						oos.flush();
					}
				}
			clientLock.unlock();
		}
		public void updateAllConvos(ArrayList<String> convos) throws IOException{
			clientLock.lock();
				for(int i=0; i<clients.size();i++){
					if( ! clients.get(i).getName().isEmpty()){
						ObjectOutputStream oos = clients.get(i).serverOut;
						oos.writeInt(UPDATE_GROUP_REQUEST);
						oos.flush();
						oos.writeObject(convos);
						oos.flush();					
					}
				}
			clientLock.unlock();
		}
		
		public void sendMessageToAll(Message msg) throws IOException{
			clientLock.lock();
				database.updateConvoContent(msg.getConversationName(), msg.getContent());
				for(int i=0; i<clients.size();i++){
					if( ! clients.get(i).getName().isEmpty()){
						ObjectOutputStream oos = clients.get(i).serverOut;
						oos.writeInt(NEW_GROUP_MESSAGE_REQUEST);
						oos.flush();
						oos.writeObject(msg);
						oos.flush();
					}
				}
			clientLock.unlock();
		}
		public void sendMessageToRecipients(Message msg) throws IOException{
			String [] recipientList = msg.getConversationName().split("@");
			clientLock.lock();
				database.updateConvoContent(msg.getConversationName(), msg.getContent());
				for(int i=0; i<clients.size();i++){
					if( ! clients.get(i).getName().isEmpty()){
						for(int j=0; j<recipientList.length;j++){
							ObjectOutputStream oos = clients.get(i).serverOut;
							oos.writeInt(NEW_PRIVATE_MESSAGE_REQUEST);
							oos.flush();
							oos.writeObject(msg);
							oos.flush();
						}

					}
				}
			clientLock.unlock();
		}
	}
}


