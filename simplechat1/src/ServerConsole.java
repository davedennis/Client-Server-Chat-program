import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import client.ChatClient;
import common.ChatIF;


public class ServerConsole extends Thread implements ChatIF {

	//Instance variables **********************************************
	
	EchoServer server;
	
	//Constructors ****************************************************	
	public ServerConsole(EchoServer server) 
	  {
	      this.server = server;   
	  }
	
	//Instance methods ************************************************
	public void run() 
	  {
	    try
	    {
	      BufferedReader fromConsole = 
	        new BufferedReader(new InputStreamReader(System.in));
	      String message;

	      while (true) 
	      {
	        message = fromConsole.readLine();
	        server.handleMessageFromServerUI(message);
	      }
	    } 
	    catch (Exception ex) 
	    {
	      System.out.println
	        ("Unexpected error while reading from console!");
	    }
	  }
	@Override
	public void display(String message) {
		System.out.println("> " + message);
		
	}

}
