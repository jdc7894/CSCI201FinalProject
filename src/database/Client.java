package database;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
	static Socket socket;
	static DataInputStream in;
	static DataOutputStream out;
	
	public static void main(String [] args) throws UnknownHostException, IOException {
		System.out.println("Connecting...");
		socket = new Socket("localhost", 7777);
		System.out.println("Connection Successful");
		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());
		Input input = new Input(in);
		Thread thread = new Thread(input);
		thread.start();
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter your name and then press enter");
		String name = sc.nextLine();
		out.writeUTF(name);
		while (true) {
			String sendMessage = sc.nextLine();
			out.writeUTF(sendMessage);
		}
	}
}

class Input implements Runnable {
	DataInputStream in;
	public Input(DataInputStream in) {
		this.in = in;
	}
	
	public void run() {
		while (true) {
			try {
				String message = in.readUTF();
				System.out.println(message);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
}
