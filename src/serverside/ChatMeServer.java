package serverside;
import java.awt.Image;
import java.io.*;
import java.net.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.Icon;

import conversation.Message;

public class ChatMeServer {
	
	public static int NEW_USER_REQUEST = 0;
	public static int LOGIN_REQUEST = 1;
	public static int SIGN_OUT_REQUEST = 2;
	public static int NEW_MESSAGE_REQUEST = 3;
	public static int INVITE_CHAT_REQUEST = 4;

	Database database;

	private Socket userReqSocket;
	private ObjectOutputStream userOut;
	private ObjectInputStream userIn;
	private Socket servReqSocket;
	private ObjectOutputStream servOut;
	private ObjectInputStream servIn;
	
	private Lock lock= new ReentrantLock();
	
	public ChatMeServer() throws IOException{
		printDbg("Starting server...");
		ServerSocket ss1 = new ServerSocket(7777);
		ServerSocket ss2 = new ServerSocket(8888);
		printDbg("Server started...");
		
		database = new Database();
		
		while(true){
			userReqSocket = ss1.accept();
			servReqSocket = ss2.accept();
			
			printDbg("Connection from: " + userReqSocket.getInetAddress());
			
			userOut = new ObjectOutputStream(userReqSocket.getOutputStream());
			userIn = new ObjectInputStream(userReqSocket.getInputStream());
			
			servOut = new ObjectOutputStream(servReqSocket.getOutputStream());
			servIn = new ObjectInputStream(servReqSocket.getInputStream());
			
			UserReqThread ct = new UserReqThread(userIn, userOut);
			ServReqThread srt = new ServReqThread(servIn, servOut);
			
			ct.setServReqThread(srt);
			srt.serUserReqThread(ct);
			
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
		
		public UserReqThread(ObjectInputStream in, ObjectOutputStream out) throws IOException{
			threadUserIn = in;
			threadUserOut = out;
		}
		
		public void setServReqThread(ServReqThread srt) {
			this.srt = srt;
		}
		
		public void run(){	
				
			//1. Send Welcome Message
			String message = "Welcome. Please Enter a Command.";
			try {
				printDbg("Welcome message Sent\n");
				threadUserOut.writeObject(message);
				
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			//2. Listen for Signal
			printDbg("SERVER: Listening for command");
			while(true){
				try {
					/* */
					int command = threadUserIn.readInt();
					handleCommand(command);
					
				} catch (IOException | ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					threadUserOut = null;
					threadUserIn = null;
				}
				
			}
		}
		private void handleCommand(int command) throws IOException, ClassNotFoundException {
			Scanner scan = new Scanner(System.in);
			printDbg("SERVER: parsing command...");
			if(command == NEW_USER_REQUEST){
				//SAT: FINISHED
				printDbg("SERVER: Command recieved on server: New User");
				String username = (String) threadUserIn.readObject();
				String password = (String) threadUserIn.readObject();
				String bio 	    = (String) threadUserIn.readObject();
				String imgPath 	= (String) threadUserIn.readObject();
				printDbg("SERVER READS:"
						+ username + " " + password + " " + bio + " " + imgPath);

				boolean OK = database.verifyUserExists(username);
				threadUserOut.writeBoolean(OK);
				threadUserOut.flush();
				if(OK == true){
					database.createAccount(username, password, bio, imgPath);
				}
				
			}
			else if(command==LOGIN_REQUEST){
				//SAT: FINISHED
				printDbg("Command recieved on server: Login\n");
				String un = (String) threadUserIn.readObject();
				String pw = (String) threadUserIn.readObject();
				printDbg("Reading in: " + un);
				printDbg("Reading in: " + pw);
				/////////////////verify//////////////////////////

				boolean OK = database.login(un, pw);
				////////////////////////////////////////////
				
				
				
				if(OK == true){
					database.addToOnlineList(un);
					printDbg("Giving OK to log in.");
					
					
					printDbg("Attempting to send online Users");
					
					threadUserOut.writeBoolean(true);
					threadUserOut.flush(); //send OK
					
					//send bio
					String bio = database.getBio(un);
					
					//send imagepath
					String imgPath = database.getImagePath(un);
					
					
					//send onlineUsers
					ArrayList<String> strArr = new ArrayList<String>();
					strArr = database.getOnlineList();
					threadUserOut.writeObject(strArr);
					printDbg("Finished command");
					
					
					return;
				}
				else{
					printDbg("Denying user.");
					threadUserOut.writeBoolean(false);
					threadUserOut.flush();
					printDbg("Finished command");
				}
			}
			
			if(command == SIGN_OUT_REQUEST){
				//SAT: FINISHED
				printDbg("Command recieved on server: Sign Out");
				String un = (String) threadUserIn.readObject();
				printDbg("Reading in: " + un);
				database.signOut(un);
				
				//TO DO: UPDATE GUI
			}
			
			if(command == NEW_MESSAGE_REQUEST){
				printDbg("Command recieved: New Message");
				printDbg("Reading message . . .");
				Message msg = (Message) threadUserIn.readObject();
				msg.print();
				String convoName = null; //this is super fake
				//String convoName = msg.getConversationName();
				String content = msg.getContent();
				boolean ok = database.verifyConvoNameExists(convoName);
				if (ok == true) {
					database.updateConvoContent(convoName, content);
					String newContent = database.getConvoContent(convoName);
					//Message newMessage = new Message(newContent, convoName);
					//UPDATE GUI
					srt.sendMessage(msg);
					
				}
				printDbg("Finished command");
			}
		}
	}
	class ServReqThread extends Thread{
		
		ObjectOutputStream servOut;
		ObjectInputStream  servIn;
		private UserReqThread ct;
		
		public ServReqThread(ObjectInputStream userIn, ObjectOutputStream servOut){
			this.servOut = servOut;
			this.servIn  = servIn;
		}
		public void run(){
			//Listen for Commands from database
			while (true) {
				
			}
			
		}
		public void serUserReqThread(UserReqThread ct) {
			this.ct = ct;
		}
		public void sendMessage(Message msg) {
			
		}
	}


}

class Database {
	//super fake
	public void doAction(int action){
		
	}
	public String getConvoContent(String convoName) {
		// TODO Auto-generated method stub
		return null;
	}
	public void updateConvoContent(String convoName, String content) {
		// TODO Auto-generated method stub
		
	}
	public boolean verifyConvoNameExists(String convoName) {
		// TODO Auto-generated method stub
		return false;
	}
	public String getImagePath(String un) {
		// TODO Auto-generated method stub
		return null;
	}
	public String getBio(String un) {
		// TODO Auto-generated method stub
		return null;
	}
	public ArrayList<String> getOnlineList() {
		// TODO Auto-generated method stub
		return null;
	}
	public void addToOnlineList(String un) {
		// TODO Auto-generated method stub
		
	}
	public boolean login(String un, String pw) {
		// TODO Auto-generated method stub
		return false;
	}
	public boolean verifyUserExists(String name){
		return true; //FIX THIS
	}
	public void createAccount(String username, String password, String bio, String imgPath){
		
	}
	
	//sign out request
	public void signOut(String username) {
		
	}
}


