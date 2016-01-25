// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

package client;

import ocsf.client.*;
import common.*;

import java.io.*;
import java.util.Scanner;

/**
 * This class overrides some of the methods defined in the abstract
 * superclass in order to give more functionality to the client.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;
 * @author Fran&ccedil;ois B&eacute;langer
 * @version July 2000
 */
public class ChatClient extends ObservableClient
{
	//Instance variables **********************************************
	String loginId;
	String channelName = "Not Connected";
	String pendingChannelName;
	boolean waitingForInviteResponse = false;
	boolean waitingForChannelName = false;
	boolean waitingForInviteName = false;

	/**
	 * The interface type variable.  It allows the implementation of 
	 * the display method in the client.
	 */
	ChatIF clientUI;


	//Constructors ****************************************************

	/**
	 * Constructs an instance of the chat client.
	 *
	 * @param host The server to connect to.
	 * @param port The port number to connect on.
	 * @param clientUI The interface type variable.
	 */

	public ChatClient(String loginId,String host, int port, ChatIF clientUI)  
	{
		super(host, port); //Call the superclass constructor
		//store login id into a instance variable.
		this.loginId = loginId;
		this.clientUI = clientUI;
		try {
			openConnection();
			sendToServer("#login " + loginId);
		} catch(IOException e) {
			clientUI.display("Cannot open connection.  Awaiting command.");
		}
	}


	//Instance methods ************************************************

	/**
	 * This method handles all data that comes in from the server.
	 *
	 * @param msg The message from the server.
	 */
	public void handleMessageFromServer(Object msg) 
	{
		String[] message = ((String)msg).split(" ");
		if(message[0].equals("#send")){
			clientUI.display(msg.toString());
		}else{
			switch(message[0]) {
			case "#quit":
				clientUI.display("SERVER SHUTTING DOWN!  DISCONNECTING!\nAbnormal termination of connection.");
				try {
					closeConnection();
				} catch (IOException e) {}
				break;
			case "#loginFail":
				clientUI.display("ERROR - Login ID is already in use.");
				System.exit(-1);
				break;
			case "#invite":
				clientUI.display(message[1] + " has invited you to their private channel named " + message[2] + ". Join? (yes/no)");
				waitingForInviteResponse = true;
				pendingChannelName = message[2];
				break;
			default:
				clientUI.display(msg.toString());
			}
		}
	}

	/**
	 * This method handles all data coming from the UI            
	 *
	 * @param message The message from the UI.    
	 */
	public void handleMessageFromClientUI(String message)
	{
		if(message.isEmpty()) {
			setChanged();
		    notifyObservers(channelName);
		    return;
		}
		String[] msg = message.split(" ");
		switch(msg[0]){
		case "#quit":
			try{
				sendToServer("#quit");
			} catch(IOException e) {}
			quit();
			break;
		case "#logoff":
			try {
				sendToServer("#quit");
				closeConnection();
				clientUI.display("Connection closed.");
			} catch (IOException e1) {}
			channelName = "Not Connected";
			break;
		case "#sethost":
			if(!isConnected()){
				setHost(msg[1]);
				clientUI.display("Host set to: " + getHost());
			}else{
				clientUI.display("Error: currently connected");
			}
			break;
		case "#setport":
			if(!isConnected()){
				if(Integer.parseInt(msg[1]) >= 0){
					setPort(Integer.parseInt(msg[1]));
					clientUI.display("Port set to: " + getPort());
				} else
					clientUI.display("Invalid Port number");	
			}else{
				clientUI.display("Error: currently connected");
			}
			break;
		case "#login":
			if(!isConnected()){
				channelName = "Global";
				try {
					openConnection();
					sendToServer("#login " + loginId);
				} catch (IOException e) {}
			}else{
				clientUI.display("Error: currently connected");
			}
			break;
		case "#gethost":
			clientUI.display(getHost());
			break;
		case "#getport":
			clientUI.display(Integer.toString(getPort()));
			break;
		case "#block":
			try {
				sendToServer(message);
			} catch (IOException e1) {

			}
			break;
		case "#channel":
			if(msg.length == 1) {
				if(channelName.isEmpty()) {
					waitingForChannelName = true;
					clientUI.display("What would you like your new channel to be called?");
				}
				else
					clientUI.display("You must leave your current channel first.");
			} 
			else
				try {
					sendToServer(message);
				} catch (IOException e1) {}
			break;
		case "#invite":
			if(msg.length == 1) {
				if(channelName.isEmpty()) {
					waitingForInviteName = true;
					clientUI.display("Who would you like to invite?");
				}
				else
					clientUI.display("You are not in a private channel.");
			}
			else
				try {
					sendToServer(message);
				} catch (IOException e1) {}
			break;
		case "#leave":
			channelName = "Global";
			try {
				sendToServer(message);
			} catch (IOException e1) {}
			break;
		default:
			if(waitingForInviteResponse) {
				if(message.equals("yes"))
					try {
						channelName = pendingChannelName;
						sendToServer("#Jo8skG3\"8mXF2bGt2D(`#jDw)JT9{# " + channelName);
						clientUI.display("You are now in the private channel named " + channelName);
						waitingForInviteResponse = false;
					} catch (IOException e) {}
				else if(message.equals("no")) {
					waitingForInviteResponse = false;
				}
				else
					clientUI.display("Would you like to join the private channel named " + pendingChannelName + "? (yes/no)");
			}
			else if(waitingForChannelName) {
				try {
					sendToServer("#channel " + msg[0]);
					waitingForChannelName = false;
				} catch (IOException e) {}
				channelName = msg[0];
			}
			else if(waitingForInviteName) {
				try {
					sendToServer("#invite " + msg[0]);
					waitingForInviteName = false;
				} catch (IOException e) {}
			}
			else
				try{
					sendToServer(message);
				} catch(IOException e){    
					clientUI.display("Could not send message to server.");
					try {
						closeConnection();
					} catch (IOException e1) {}
					clientUI.display("Connection closed.");
				}
		}
		setChanged();
	    notifyObservers(channelName);
	}

	/**
	 * This method terminates the client.
	 */
	public void quit()
	{
		try
		{
			closeConnection();
		}
		catch(IOException e) {}
		System.exit(0);
	}

}
//End of ChatClient class
