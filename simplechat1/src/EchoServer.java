// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import client.ChatClient;
import ocsf.server.*;

/**
 * This class overrides some of the methods in the abstract 
 * superclass in order to give more functionality to the server.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;re
 * @author Fran&ccedil;ois B&eacute;langer
 * @author Paul Holden
 * @version July 2000
 */
public class EchoServer extends ObservableServer 
{
	//Class variables *************************************************
	private ArrayList<String> clients = new ArrayList<String>();
	private Set<String> currentClients = new HashSet<String>();
	private ArrayList<String> blockedServerClients = new ArrayList<String>();
	private HashMap<ConnectionToClient, ArrayList<String>> blockedClients = new HashMap<ConnectionToClient, ArrayList<String>>();
	private HashMap<ConnectionToClient, ArrayList<String>> channelMembers = new HashMap<ConnectionToClient, ArrayList<String>>();
	/**
	 * The default port to listen on.
	 */
	final public static int DEFAULT_PORT = 5555;

	//Constructors ****************************************************

	/**
	 * Constructs an instance of the echo server.
	 *
	 * @param port The port number to connect on.
	 */
	public EchoServer(int port) 
	{
		super(port);
	}

	//Instance variables ************************************************
	public boolean isClosed;

	//Instance methods ************************************************


	public void handleMessageFromServerUI(String message){
		String[] msg = message.split(" ");
		switch(msg[0]) {
		case "#quit":
			this.sendToAllClients("#quit");
			try {
				this.close();
			} catch (IOException e1) {}
			System.exit(0);
			break;
		case "#stop":
			this.sendToAllClients("WARNING - Server has stopped listening for connections.");
			this.stopListening();
			break;
		case "#close":
			try {
				this.sendToAllClients("#quit");
				this.close();
			} catch (IOException e) {}
			break;
		case "#setport":
			if(this.isClosed){
				this.setPort(Integer.parseInt(msg[1]));
				System.out.println("Port set to: " + getPort());
			}else{
				System.out.println("Error: Can not set port when server is open");
			}
			break;
		case "#start":
			this.isClosed = false;
			if(!this.isListening())
				try {
					this.listen();
				} catch (IOException e) {}
			System.out.println("Server listening for connections on port " + getPort());
			break;
		case "#getport":
			System.out.println(this.getPort());
			break;
		case "#block":
			if(msg.length == 1)
				break;
			if(msg[1].equals("server")){
					System.out.println("You cannot block the sending of messages to yourself.");
			}
			else {
				boolean foundClient = false;
				for(int i = 0; i < clients.size();i++) {
					if(msg[1].equals(clients.get(i)))
						foundClient = true;
				}
				if(foundClient && !blockedServerClients.contains(msg[1])) {
					blockedServerClients.add(msg[1]);
					System.out.println("Messages from " + msg[1] + " will be blocked.");
				}
				else {
					if(foundClient)
						System.out.println("Messages from " + msg[1] + " were already blocked.");
					else {
						System.out.println("User " + msg[1] + " does not exist.");
					}
				}
			}
		break;
		case "#unblock":
			if(msg.length == 1) {
				if(blockedServerClients.isEmpty())
						System.out.println("No blocking is in effect.");
				else {
					for(int index = 0; index < blockedServerClients.size(); index++) {
							System.out.println("Messages from " + blockedServerClients.get(index) + " will now be displayed.");
					}
					blockedServerClients.clear();
				}
			}
			else {
				if(blockedServerClients.contains(msg[1])) {
					blockedServerClients.remove(blockedServerClients.indexOf(msg[1]));
						System.out.println("Messages from " + msg[1] + " will now be displayed.");
				} else
						System.out.println("Messages from " + msg[1] + " were not blocked.");
			}
			break;
		case "#whoiblock":
			if(blockedServerClients.isEmpty())
					System.out.println("No blocking is in effect.");
			else {
				for(int index = 0; index < blockedServerClients.size(); index++) {
						System.out.println("Messages from " + blockedServerClients.get(index) + " are blocked.");
				}
			}
			break;
		case "#whoblocksme":
			for(ConnectionToClient c: blockedClients.keySet()) {
				ArrayList<String> currentBlockedClients = blockedClients.get(c);
				if(currentBlockedClients.contains("server"))
						System.out.println("Messages to " + c.getInfo("loginID") + " are being blocked.");
			}
			break;
		default:
			String prefix = "SERVER MESSAGE> ";
			message = prefix.concat(message);
			this.sendToAllClients(message);
			System.out.println(message);
			break;

		}
	}
	/**
	 * This method handles any messages received from the client.
	 *
	 * @param msg The message received from the client.
	 * @param client The connection from which the message originated.
	 */
	public void handleMessageFromClient(Object message, ConnectionToClient client)
	{
		client.setInfo("lastMessage", System.nanoTime()); //set time for clients last sent message
		String[] msg = message.toString().split(" ");
		switch(msg[0]){
		case "#quit":
			clientDisconnected(client);
			currentClients.remove(client.getInfo("loginID"));
			break;
		case "#login":
			if(currentClients.add(msg[1])) {
				client.setInfo("loginID", msg[1]);
				client.setInfo("status", "available");
				System.out.println("Message received: " + message + " from \"" + client.getInfo("loginID") + "\" " + client);
				System.out.println(msg[1] + " has logged on.");
				this.sendToAllClients(msg[1] + " has logged on.");
				clients.add(msg[1]);
			}
			else {
				try {
					System.out.println("New client failed to connect with duplicate ID " + msg[1] + ".");
					client.sendToClient("#loginFail");
				} catch (IOException e) {}
			}
			break;
		case "#block":
			if(msg.length == 1)
				break;
			if(msg[1].equals(client.getInfo("loginID"))){
				try {
					client.sendToClient("You cannot block the sending of messages to yourself.");
				} catch (IOException e) {}
			}
			else{
				boolean foundClient = false;
				for(int i = 0; i < clients.size();i++){
					if(msg[1].equals(clients.get(i))){
						foundClient = true;
						if(blockedClients.get(client) == null)
							blockedClients.put(client, new ArrayList<String>());
						ArrayList<String> currentBlockedClients = blockedClients.get(client);
						if(currentBlockedClients.contains(msg[1])) {
							try {
								client.sendToClient("Messages from " + msg[1] + " were already blocked.");
							} catch (IOException e) {}
						}
						else {
							currentBlockedClients.add(msg[1]);
							try {
								client.sendToClient("Messages from " + msg[1] + " will be blocked.");
							} catch (IOException e) {}
							break;
						}
					}
				}
				if(foundClient == false)
					try {
						client.sendToClient("User " + msg[1] + " does not exist.");
					} catch (IOException e) {}
			}

			break;
		case "#unblock":
			if(msg.length == 1) {
				ArrayList<String> blocked = blockedClients.get(client);
				if(blocked == null || blocked.isEmpty())
					try {
						client.sendToClient("No blocking is in effect.");
					} catch (IOException e) {}
				else {
					for(int index = 0; index < blocked.size(); index++) {
						try {
							client.sendToClient("Messages from " + blocked.get(index) + " will now be displayed.");
						} catch (IOException e) {}
					}
					blocked.clear();
				}
			}
			else {
				ArrayList<String> blocked = blockedClients.get(client);
				if(blocked.contains(msg[1])) {
					blocked.remove(blocked.indexOf(msg[1]));
					try {
						client.sendToClient("Messages from " + msg[1] + " will now be displayed.");
					} catch (IOException e) {}
				} else
					try {
						client.sendToClient("Messages from " + msg[1] + " were not blocked.");
					} catch (IOException e) {}
			}
			break;
		case "#whoiblock":
			ArrayList<String> blocked = blockedClients.get(client);
			if(blocked == null || blocked.isEmpty())
				try {
					client.sendToClient("No blocking is in effect.");
				} catch (IOException e) {}
			else {
				for(int index = 0; index < blocked.size(); index++) {
					try {
						client.sendToClient("Messages from " + blocked.get(index) + " are blocked.");
					} catch (IOException e) {}
				}
			}
			break;
		case "#whoblocksme":
			for(ConnectionToClient c: blockedClients.keySet()) {
				ArrayList<String> currentBlockedClients = blockedClients.get(c);
				if(currentBlockedClients.contains(client.getInfo("loginID")))
					try {
						client.sendToClient("Messages to " + c.getInfo("loginID") + " are being blocked.");
					} catch (IOException e) {}
			}
			break;
		case "#private":
			if(msg.length >= 3) {
				if(currentClients.contains(msg[1])) {
					if(msg[1].equals(client.getInfo("loginID")))
						try {
							client.sendToClient("Cannot send a private message to yourself.");
						} catch (IOException e) {}
					else {
						System.out.println("Private message sent from " + client + " to " + msg[1]);
						sendPrivateMessage(message, client.getInfo("loginID"));
					}
				}
				else {
					try {
						client.sendToClient("Client name " + msg[1] + " does not exist.");
					} catch (IOException e) {}
				}
			}
			else
				try {
					client.sendToClient("How to send private message: #private <user> <message>");
				} catch (IOException e) {}
			break;
		case "#channel":
			if(msg.length == 2) {
				if(client.getInfo("channel") == null) {
					if(channelNameIsUnique(msg[1]))
						try {
							client.sendToClient("Created new private channel named " + msg[1] + ".");
							channelMembers.put(client, new ArrayList<String>());
							client.setInfo("channel", msg[1]);
							client.setInfo("channelOwner", "yes");
						} catch (IOException e) {}
					else
						try {
							client.sendToClient("A private channel with that name already exists.");
						} catch (IOException e) {}
				}
				else
					try {
						client.sendToClient("You must leave your current channel first.");
					} catch (IOException e) {}
			}
			else
				try {
					client.sendToClient("How to create a private channel: #channel <channelName>");
				} catch (IOException e) {}
			break;
		case "#invite":
			if(client.getInfo("channel") == null) {
				try {
					client.sendToClient("You are not in a private channel.");
				} catch (IOException e) {}
			}
			else if(client.getInfo("channelOwner").equals("no"))
				try {
					client.sendToClient("Only the channel owner can invite users.");
				} catch (IOException e) {}
			else if(msg.length == 2) {
				if(client.getInfo("loginID").equals(msg[1]))
					try {
						client.sendToClient("You cannot invite yourself to a private channel.");
					} catch (IOException e) {}
				else if(currentClients.contains(msg[1])) {
					ArrayList<String> currentMembers = channelMembers.get(client);
					if(currentMembers.contains(msg[1]))
						try {
							client.sendToClient(msg[1] + " is already a member of your channel.");
						} catch (IOException e) {}
					else {
						boolean isBlocked = false;
						for(ConnectionToClient c: blockedClients.keySet()) {
							ArrayList<String> currentBlockedClients = blockedClients.get(c);
							if(currentBlockedClients.contains(client.getInfo("loginID")))
								isBlocked = true;
						}
						if(!isBlocked) {
							sendChannelInvite(client, msg[1]);
							try {
								client.sendToClient(msg[1] + " has been invited to your channel.");
							} catch (IOException e) {}
						}
						else
							try {
								client.sendToClient(msg[1] + " is currently blocking you.");
							} catch (IOException e) {}
					}
				}
				else
					try {
						client.sendToClient("User " + msg[1] + " does not exist.");
					} catch (IOException e) {}
			}
			else
				try {
					client.sendToClient("How to invite someone to your private channel: #invite <user>");
				} catch (IOException e) {}
			break;
		case "#Jo8skG3\"8mXF2bGt2D(`#jDw)JT9{#": //random key to accept private channel invites
			if(client.getInfo("channel") != null) {
				if(client.getInfo("channelOwner").equals("yes")) {
					sendChannelMessage(client, "ATTENTION: " + client.getInfo("channel") + " has been disbanded by the owner.");
					channelMembers.put(client, null);
					disbandChannel(client.getInfo("channel"));
					try {
						client.sendToClient("Your private channel has been disbanded.");
					} catch (IOException e) {}
				}
				else {
					sendChannelMessage(client, "ATTENTION: " + client.getInfo("loginID") + " has left this private channel.");
					for(ConnectionToClient c: blockedClients.keySet()) {
						if(c.getInfo("channel").equals(client.getInfo("channel")) && c.getInfo("channel").equals("yes"))
							channelMembers.get(c).remove(client.getInfo("loginID"));
					}
					client.setInfo("channel", null);
					client.setInfo("channelOwner", null);
					try {
						client.sendToClient("You have left the private channel.");
					} catch (IOException e) {}
				}
			}
			sendChannelAccept(client, msg[1]);
			client.setInfo("channel", msg[1]);
			client.setInfo("channelOwner", "no");
			break;
		case "#leave":
			if(client.getInfo("channel") == null)
				try {
					client.sendToClient("You are not in a private channel.");
				} catch (IOException e) {}
			else if(client.getInfo("channelOwner").equals("yes")) {
				sendChannelMessage(client, "ATTENTION: " + client.getInfo("channel") + " has been disbanded by the owner.");
				channelMembers.put(client, null);
				disbandChannel(client.getInfo("channel"));
				try {
					client.sendToClient("Your private channel has been disbanded.");
				} catch (IOException e) {}
			}
			else {
				sendChannelMessage(client, "ATTENTION: " + client.getInfo("loginID") + " has left this private channel.");
				Thread[] clientThreadList = getClientConnections();		
				for (int i=0; i<clientThreadList.length; i++){
					//check for owner of channel
					if(((ConnectionToClient)clientThreadList[i]).getInfo("channel").equals(client.getInfo("channel")) && ((ConnectionToClient)clientThreadList[i]).getInfo("channelOwner").equals("yes"))
						channelMembers.get(((ConnectionToClient)clientThreadList[i])).remove(client.getInfo("loginID"));
				}
				client.setInfo("channel", null);
				client.setInfo("channelOwner", null);
				try {
					client.sendToClient("You have left the private channel.");
				} catch (IOException e) {}
			}
			break;
		case "#global":
			this.sendToAllClients(client.getInfo("loginID") + "> " + ((String)message).substring(8));
			break;
		case "#available":
			client.setInfo("status", "available");
			try {
				client.sendToClient("Your status has been set to available");
			} catch (IOException e1) {}
			break;
		case "#notavailable":
			client.setInfo("status", "unavailable");
			try{
				client.sendToClient("Your stauts has been set to unavailable");
			} catch(IOException e1) {}
			break;
		case "#forward":
			if(currentClients.contains(msg[1])) {
				//can't send to yourself...
				if(msg[1].equals(client.getInfo("loginID"))){
					try {
						client.sendToClient("Cannot forward a message to yourself.");
					} catch (IOException e) {}
				}
				//check if the receiver is blocking the sender
				boolean isBlocked = false;
				for(ConnectionToClient c: blockedClients.keySet()) {
					ArrayList<String> currentBlockedClients = blockedClients.get(c);
					if(currentBlockedClients.contains(client.getInfo("loginID")))
						try {
							client.sendToClient("Cannot foward messages to " + msg[1] + " because they are blocking you");
							isBlocked = true;
						} catch (IOException e) {}
				}
				if(!isBlocked){
					Thread[] clientThreadList = getClientConnections();
					for (int i=0; i<clientThreadList.length; i++) {
						try {						
							if(((ConnectionToClient)clientThreadList[i]).getInfo("loginID").equals(msg[1])){
								client.setInfo("forward", ((ConnectionToClient)clientThreadList[i]).getInfo("loginID"));
								client.sendToClient("Private and Channel messages sent to you will now be forwarded to " + msg[1]);
							}
						} catch (Exception ex) {}	
					}
				}
	
			}
			else{
				try {
					client.sendToClient("Client name " + msg[1] + " does not exist.");
				} catch (IOException e) {}
			}
			break;
		case "#unforward":
			String fwd = client.getInfo("forward").toString();
			client.setInfo("forward", null);
			try {
				client.sendToClient("Messages will no longer be forwarded to " + fwd);
			} catch (IOException e) {}
			break;
		case "#status":
			Thread[] clientThreadList = getClientConnections();
			if(msg.length < 2){
				try {
					client.sendToClient("To get status of user or channel: #status <userID/channelName>");
				} catch (IOException e) {}
			}
			if(clients.contains(msg[1])){
				if(currentClients.contains(msg[1])){
					for (int i=0; i<clientThreadList.length; i++) {
						try {						
							if(((ConnectionToClient)clientThreadList[i]).getInfo("loginID").equals(msg[1])){
								String status  = getClientStatus(((ConnectionToClient)clientThreadList[i]));
								client.sendToClient("User " + msg[1] + " is " + status);
							}
						} catch (Exception ex) {}	
					}
					break;
				}else{
					try {
						client.sendToClient("User " + msg[1] + " is Offline");
					} catch (IOException e) {}
					break;
				}
			//get channel member status because no user found	
			}else{
				if(client.getInfo("channel") == null || !client.getInfo("channel").toString().equals(msg[1]) ){
					try {
						client.sendToClient("You are not authorized to get information about channel " + msg[1]);
						break;
					} catch (IOException e) {}
				}else{		
					for (int j=0; j<clientThreadList.length; j++) {
						if(((ConnectionToClient)clientThreadList[j]).getInfo("channel") != null && ((ConnectionToClient)clientThreadList[j]).getInfo("channel").equals(msg[1])){
							if(((ConnectionToClient)clientThreadList[j]) != client){
								String status = getClientStatus(((ConnectionToClient)clientThreadList[j]));
								try {
									client.sendToClient("User " + ((ConnectionToClient)clientThreadList[j]).getInfo("loginID") + " is " + status);
								} catch (IOException e) {}
							}
						}
					}
					
				}
			}
			break;
		case "#send":
			this.sendToAllClients(message);
			break;
		default:
			if(!blockedServerClients.contains(client.getInfo("loginID")))
				System.out.println("Message received: " + message + " from \"" + client.getInfo("loginID") + "\" " + client);
			if(client.getInfo("channel") == null)
				this.sendToAllClients(client.getInfo("loginID") + "> " + message);
			else
				sendChannelMessage(client, client.getInfo("loginID") + "> " + message);
		}
		setChanged();
	    notifyObservers(message);
	}
	
	private String getClientStatus(ConnectionToClient client){
		if(client.getInfo("status").toString().equals("available")){
			long systemTime = System.nanoTime();
			long downTime = systemTime - (long)client.getInfo("lastMessage");
			double downTimeSec = (double)downTime / 1000000000.0;
			if(downTimeSec > 300){
				return "Idle";
			}else{
				return "Online";
			}
		}else{
			return "unavailable";
		}
	}
	
	private boolean channelNameIsUnique(Object channelName) {
		boolean unique = true;
		Thread[] clientThreadList = getClientConnections();
		for (int i=0; i<clientThreadList.length; i++) {
			if(((ConnectionToClient)clientThreadList[i]).getInfo("channel") != null && ((ConnectionToClient)clientThreadList[i]).getInfo("channel").equals(channelName)) {
				unique = false;
				break;
			}	
		}
		return unique;
	}

	private void disbandChannel(Object channelName) {
		Thread[] clientThreadList = getClientConnections();
		for (int i=0; i<clientThreadList.length; i++) {
			if(((ConnectionToClient)clientThreadList[i]).getInfo("channel") != null && ((ConnectionToClient)clientThreadList[i]).getInfo("channel").equals(channelName)) {
				((ConnectionToClient)clientThreadList[i]).setInfo("channel", null);
				((ConnectionToClient)clientThreadList[i]).setInfo("channelOwner", null);
			}
		}
	}

	public void sendChannelMessage(ConnectionToClient client, Object msg) {
		Thread[] clientThreadList = getClientConnections();
		String channel = (String)client.getInfo("channel");
		String sender = (String)client.getInfo("loginID");
		for (int i=0; i<clientThreadList.length; i++) {
			try {
				ArrayList<String> blocked = blockedClients.get(((ConnectionToClient)clientThreadList[i]));
				if(!sender.equals(((ConnectionToClient)clientThreadList[i]).getInfo("loginID")) && (blocked == null || !blocked.contains(sender)) && ((ConnectionToClient)clientThreadList[i]).getInfo("channel").equals(channel)){
					((ConnectionToClient)clientThreadList[i]).sendToClient(msg);
				//check if recipient is forwarding messages
					if( ((ConnectionToClient)clientThreadList[i]).getInfo("forward") != null ){
						String forwardTo = ((ConnectionToClient)clientThreadList[i]).getInfo("forward").toString();	
						for (int j=0; j<clientThreadList.length; j++){
							if(((ConnectionToClient)clientThreadList[j]).getInfo("loginID").equals(forwardTo)){
								String realMessage = msg.toString().substring(sender.length()+2);
								((ConnectionToClient)clientThreadList[j]).sendToClient("Forwarded from " + ((ConnectionToClient)clientThreadList[i]).getInfo("loginID") + "> " + realMessage);
							}
						}
						
					}
				}
			} catch (Exception ex) {}
		}
	}

	public void sendChannelAccept(ConnectionToClient client, Object channelName) {
		Thread[] clientThreadList = getClientConnections();
		for (int i=0; i<clientThreadList.length; i++) {
			try {
				if(((ConnectionToClient)clientThreadList[i]).getInfo("channel").equals(channelName)) {
					((ConnectionToClient)clientThreadList[i]).sendToClient(client.getInfo("loginID") + " has joined your channel");
					if(((ConnectionToClient)clientThreadList[i]).getInfo("channelOwner").equals("yes"))
						channelMembers.get((ConnectionToClient)clientThreadList[i]).add((String)client.getInfo("loginID"));
				}
			} catch (Exception ex) {}
		}
	}

	public void sendChannelInvite(ConnectionToClient channelOwner, Object invitee) {
		Thread[] clientThreadList = getClientConnections();
		for (int i=0; i<clientThreadList.length; i++) {
			try {
				if(((ConnectionToClient)clientThreadList[i]).getInfo("loginID").equals(invitee))
					((ConnectionToClient)clientThreadList[i]).sendToClient("#invite " + channelOwner.getInfo("loginID") + " " + channelOwner.getInfo("channel"));
			} catch (Exception ex) {}
		}
	}

	public void sendPrivateMessage(Object msg, Object sender) {
		Thread[] clientThreadList = getClientConnections();
		String m = (String)msg;
		for (int i=0; i<clientThreadList.length; i++) {
			try {
				String receiver = ((String)msg).split(" ")[1];
				ArrayList<String> blocked = blockedClients.get(((ConnectionToClient)clientThreadList[i]));
				if(((ConnectionToClient)clientThreadList[i]).getInfo("loginID").equals(receiver) && (blocked == null || !blocked.contains(sender))){
					((ConnectionToClient)clientThreadList[i]).sendToClient((String)sender + "> " + m.substring(m.indexOf(receiver) + receiver.length() + 1));
				//check if recipient is forwarding messages
					if( ((ConnectionToClient)clientThreadList[i]).getInfo("forward") != null ){
						String forwardTo = ((ConnectionToClient)clientThreadList[i]).getInfo("forward").toString();	
						for (int j=0; j<clientThreadList.length; j++){
							if(((ConnectionToClient)clientThreadList[j]).getInfo("loginID").equals(forwardTo)){
								//check if end receiver is blocked by forwarder
								ConnectionToClient endUser = ((ConnectionToClient)clientThreadList[j]);
								ConnectionToClient forwarder =((ConnectionToClient)clientThreadList[i]);
								
								ArrayList<String> blockedPeeps = blockedClients.get(endUser);
								if(blockedPeeps == null || !blockedPeeps.contains(forwarder.getInfo("loginID"))){
									((ConnectionToClient)clientThreadList[j]).sendToClient("Forwarded from " +(String)receiver + "> " + m.substring(m.indexOf(receiver) + receiver.length()));
								}
							}
						}
						
					}
				}
			} catch (Exception ex) {}
		}
	}

	public void sendToAllClients(Object msg) {
		String[] message = msg.toString().split(" ");
		if(message[0].equals("#send")){
			Thread[] clientThreadList = getClientConnections();
			for (int i=0; i<clientThreadList.length; i++){
					try {
						((ConnectionToClient)clientThreadList[i]).sendToClient(msg);
					} catch (IOException e) {}
			}
		}else{
			Thread[] clientThreadList = getClientConnections();

			for (int i=0; i<clientThreadList.length; i++) {
				try {
					String sender = ((String)msg).split(">")[0];
					if(sender.equals("SERVER MESSAGE"))
						sender = "server";
					ArrayList<String> blocked = blockedClients.get(((ConnectionToClient)clientThreadList[i]));
					if(blocked == null || !blocked.contains(sender))
						((ConnectionToClient)clientThreadList[i]).sendToClient(msg);
				} catch (Exception ex) {}
			}
		}
	}

	/**
	 * This method overrides the one in the superclass.  Called
	 * when the server starts listening for connections.
	 */
	protected void serverStarted()
	{
		System.out.println
		("Server listening for connections on port " + getPort());
		setChanged();
	    notifyObservers(SERVER_STARTED);
	}

	/**
	 * This method overrides the one in the superclass.  Called
	 * when the server stops listening for connections.
	 */
	protected void serverStopped()
	{
		System.out.println
		("Server has stopped listening for connections.");
		setChanged();
	    notifyObservers(SERVER_STOPPED);
	}

	protected void serverClosed() {
		this.isClosed = true;
		setChanged();
	    notifyObservers(SERVER_CLOSED);
	}
	
	/**
	 * This method overrides the one in the superclass.  Called
	 * when a client connects to the server.
	 */
	protected void clientConnected(ConnectionToClient client) 
	{
		System.out.println("A new client is attempting to connect to the server.");
		setChanged();
	    notifyObservers(CLIENT_CONNECTED);
	}

	/**
	 * This method overrides the one in the superclass.  Called
	 * when a client disconnects from the server.
	 */
	synchronized protected void clientDisconnected(ConnectionToClient client) 
	{
		if(client != null)
			System.out.println(client.getInfo("loginID") + " has disconnected");
		this.sendToAllClients(client.getInfo("loginID") + " has disconnected");
		currentClients.remove(client.getInfo("loginID"));
		setChanged();
	    notifyObservers(CLIENT_DISCONNECTED);
	}


	//Class methods ***************************************************

	/**
	 * This method is responsible for the creation of 
	 * the server instance (there is no UI in this phase).
	 *
	 * @param args[0] The port number to listen on.  Defaults to 5555 
	 *          if no argument is entered.
	 */
	public static void main(String[] args) 
	{

		int port = 0; //Port to listen on

		try
		{
			port = Integer.parseInt(args[1]); //Get port from command line
		}
		catch(Throwable t)
		{
			port = DEFAULT_PORT; //Set port to 5555
		}

		EchoServer sv = new EchoServer(port);
		ServerConsole console = new ServerConsole(sv);
		console.start();

		try {
			Scanner scanner = new Scanner(new File(args[0]));
			while(scanner.hasNextLine()) {
				sv.clients.add(scanner.nextLine());
			}
			scanner.close();
			sv.clients.add("server");
		} catch (Exception e) {
			System.out.println("ERROR - Bad filename.");
			System.exit(-1);
		}

		try 
		{
			sv.listen(); //Start listening for connections
		} 
		catch (Exception ex) 
		{
			System.out.println("ERROR - Could not listen for clients!");
			System.exit(-1);
		}
	}
}
//End of EchoServer class
