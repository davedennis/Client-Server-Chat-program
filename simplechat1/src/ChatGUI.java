import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import client.*;
import common.*;
import drawpad.DrawPad;
import drawpad.OpenDrawPad;

/**
 * This class constructs the UI for a chat client.  It implements the
 * chat interface in order to activate the display() method.
 * Warning: Some of the code here is cloned in ServerConsole 
 *
 * @author Fran&ccedil;ois B&eacute;langer
 * @author Dr Timothy C. Lethbridge  
 * @author Dr Robert Lagani&egrave;re
 * @version July 2000
 */
public class ChatGUI implements ChatIF, Observer
{
    //Class variables *************************************************

    /**
     * The default port to connect on.
     */
    final public static int DEFAULT_PORT = 5555;

    //Instance variables **********************************************

    /**
     * The instance of the client that created this ConsoleChat.
     */
    private ChatClient client;
    private JTextArea messageBox = new JTextArea("Global Chat Room\n");
    private Font buttonFont = new Font("Arial", Font.PLAIN, 11);
    private TitledBorder currentChannel = BorderFactory.createTitledBorder("Not Connected");
    public OpenDrawPad openDP;
    public ChatGUI thisGUI = this;

    //Constructors ****************************************************

    /**
     * Constructs an instance of the ClientConsole UI.
     *
     * @param host The host to connect to.
     * @param port The port to connect on.
     */
    public ChatGUI(String loginId,String host, int port) 
    {
        client= new ChatClient(loginId, host, port, this);
        client.addObserver(this);
        client.handleMessageFromClientUI("");
    }


    //Instance methods ************************************************

    /**
     * This methd displays the GUI and waits for text input.  Once it is 
     * received, it sends it to the client's message handler.
     */
    public void accept() 
    {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame frame = new JFrame("Simple Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        /** Create message and text input panel */
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        
        // Message box setup
        messageBox.setEditable(false);
        messageBox.setLineWrap(true);
        JScrollPane scrollMessageBox = new JScrollPane(messageBox);
        scrollMessageBox.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollMessageBox.setPreferredSize(new Dimension(500, 300));
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        panel.add(scrollMessageBox, constraints);
        
        // Text input setup
        JTextField textInput = new JTextField();
        textInput.setPreferredSize(new Dimension(400, 33));
        textInput.setBorder(currentChannel);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        panel.add(textInput, constraints);
        
        JButton sendInput = new JButton("Send");
        sendInput.setFont(buttonFont);
        sendInput.setPreferredSize(new Dimension(40,30));
        sendInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(textInput.getText().length() > 0){
                    //messageBox.append(textInput.getText() + "\n");
                	client.handleMessageFromClientUI(textInput.getText());
                	textInput.setText("");
                }
            }
        });
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 1;
        panel.add(sendInput, constraints);
        textInput.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER)
                    sendInput.getActionListeners()[0].actionPerformed(null);
            }
            @Override
            public void keyReleased(KeyEvent e) {}
            @Override
            public void keyTyped(KeyEvent e) {}
        });
        frame.getContentPane().add(panel, BorderLayout.WEST);
        
        /** Create side button panel */
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setPreferredSize(new Dimension(200, 300));
        
        // Channel button
        JPopupMenu channelButtons = new JPopupMenu("Channel");
        channelButtons.setPreferredSize(new Dimension(150, 100));
        JMenuItem createChannelButton = new JMenuItem("Create Channel");
        createChannelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame chanframeInv = new JFrame("Channel Name");
                JPanel chanInvName = new JPanel();
                GridBagConstraints chanconstraints = new GridBagConstraints();
                
                
                JTextField channametextInput = new JTextField();
                channametextInput.setPreferredSize(new Dimension(400, 33));
                channametextInput.setBorder(currentChannel);
                constraints.fill = GridBagConstraints.BOTH;
                constraints.gridx = 0;
                constraints.gridy = 1;
                constraints.gridwidth = 1;
                chanInvName.add(channametextInput, chanconstraints);
                
                chanframeInv.getContentPane().add(chanInvName);
                
                
                JButton sendInputChanName = new JButton("Create Channel");
                sendInputChanName.setFont(buttonFont);
                sendInputChanName.setPreferredSize(new Dimension(200,33));
                sendInputChanName.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        
                        client.handleMessageFromClientUI("#channel" + " " + channametextInput.getText());
                        chanframeInv.dispose();
                        frame.setVisible(true);
                    }
                });
                constraints.fill = GridBagConstraints.BOTH;
                constraints.gridx = 1;
                constraints.gridy = 1;
                chanInvName.add(sendInputChanName, constraints);
                
                chanframeInv.pack();
                chanframeInv.setVisible(true);
                //client.handleMessageFromClientUI("#channel");
            }
        });
        channelButtons.add(createChannelButton);
        JMenuItem inviteButton = new JMenuItem("Invite");
        inviteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame chanframeInvUser = new JFrame("Invite user to channel");
                JPanel chanInvNameUser = new JPanel();
                GridBagConstraints chanconstraints = new GridBagConstraints();
                
                
                JTextField channametextInputUser = new JTextField();
                channametextInputUser.setPreferredSize(new Dimension(400, 33));
                channametextInputUser.setBorder(currentChannel);
                constraints.fill = GridBagConstraints.BOTH;
                constraints.gridx = 0;
                constraints.gridy = 1;
                constraints.gridwidth = 1;
                chanInvNameUser.add(channametextInputUser, chanconstraints);
                
                chanframeInvUser.getContentPane().add(chanInvNameUser);
                
                
                JButton sendInputChanNameUser = new JButton("Invite User");
                sendInputChanNameUser.setFont(buttonFont);
                sendInputChanNameUser.setPreferredSize(new Dimension(200,33));
                sendInputChanNameUser.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        
                        client.handleMessageFromClientUI("#invite" + " " + channametextInputUser.getText());
                        chanframeInvUser.dispose();
                        frame.setVisible(true);
                    }
                });
                constraints.fill = GridBagConstraints.BOTH;
                constraints.gridx = 1;
                constraints.gridy = 1;
                chanInvNameUser.add(sendInputChanNameUser, constraints);
                
                chanframeInvUser.pack();
                chanframeInvUser.setVisible(true);
                //client.handleMessageFromClientUI("#channel");
            
            }
        });
        channelButtons.add(inviteButton);
        JMenuItem statusButton = new JMenuItem("Status");
        statusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	JFrame statusFrame = new JFrame("Status of User");
                JPanel statusPan = new JPanel();
                GridBagConstraints statuscon = new GridBagConstraints();
                
                
                JTextField statustext = new JTextField();
                statustext.setPreferredSize(new Dimension(400, 33));
                statustext.setBorder(currentChannel);
                statuscon.fill = GridBagConstraints.BOTH;
                statuscon.gridx = 0;
                statuscon.gridy = 1;
                statuscon.gridwidth = 1;
                statusPan.add(statustext, statuscon);
                
                statusFrame.getContentPane().add(statusPan);
                
                
                JButton userstatusbutton = new JButton("user status");
                userstatusbutton.setFont(buttonFont);
                userstatusbutton.setPreferredSize(new Dimension(200,33));
                userstatusbutton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        client.handleMessageFromClientUI("#status" + " " + statustext.getText());
                        statusFrame.dispose();
                        frame.setVisible(true);
                        
                    }
                });
                statuscon.fill = GridBagConstraints.BOTH;
                statuscon.gridx = 1;
                statuscon.gridy = 1;
                statusPan.add(userstatusbutton, statuscon);
                
                
                JTextField chantext = new JTextField();
                chantext.setPreferredSize(new Dimension(400, 33));
                chantext.setBorder(currentChannel);
                statuscon.fill = GridBagConstraints.BOTH;
                statuscon.gridx = 0;
                statuscon.gridy = 1;
                statuscon.gridwidth = 1;
                statusPan.add(chantext, statuscon);
                
                //statusFrame.getContentPane().add(statusPan);
                
                JButton channelstatusbutton = new JButton("channel status");
                channelstatusbutton.setFont(buttonFont);
                channelstatusbutton.setPreferredSize(new Dimension(200,33));
                channelstatusbutton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        client.handleMessageFromClientUI("#status" + " " + chantext.getText());
                        statusFrame.dispose();
                        frame.setVisible(true);
                        
                    }
                });
                statuscon.fill = GridBagConstraints.BOTH;
                statuscon.gridx = 1;
                statuscon.gridy = 1;
                statusPan.add(channelstatusbutton, statuscon);
                
                statusFrame.pack();
                statusFrame.setVisible(true);
                //client.handleMessageFromClientUI("#channel");
            }
        });
        channelButtons.add(statusButton);
        JMenuItem leaveButton = new JMenuItem("Leave");
        leaveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	client.handleMessageFromClientUI("#leave");
            }
        });
        channelButtons.add(leaveButton);
        JButton channelButton = new JButton("Channel");
        channelButton.setPreferredSize(new Dimension(150, 50));
        channelButton.setFont(buttonFont);
        channelButton.setComponentPopupMenu(channelButtons);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        buttonPanel.add(channelButton, constraints);
        
        // Block button
        JPopupMenu blockButtons = new JPopupMenu("Channel");
        blockButtons.setPreferredSize(new Dimension(150, 100));
        JMenuItem innerBlockButton = new JMenuItem("Block");
        innerBlockButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                JFrame blockUserFrame = new JFrame("Block User");
                JPanel blcokUserPan = new JPanel();
                GridBagConstraints blockuserCon = new GridBagConstraints();
                
                
                JTextField blockusertext = new JTextField();
                blockusertext.setPreferredSize(new Dimension(400, 33));
                blockusertext.setBorder(currentChannel);
                blockuserCon.fill = GridBagConstraints.BOTH;
                blockuserCon.gridx = 0;
                blockuserCon.gridy = 1;
                blockuserCon.gridwidth = 1;
                blcokUserPan.add(blockusertext , blockuserCon);
                
                blockUserFrame.getContentPane().add(blcokUserPan);
                
                
                JButton blockuserbutton = new JButton("block");
                blockuserbutton.setFont(buttonFont);
                blockuserbutton.setPreferredSize(new Dimension(200,33));
                blockuserbutton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        
                        client.handleMessageFromClientUI("#block" + " " + blockusertext .getText());
                        blockUserFrame.dispose();
                        frame.setVisible(true);
                    }
                });
                blockuserCon.fill = GridBagConstraints.BOTH;
                blockuserCon.gridx = 1;
                blockuserCon.gridy = 1;
                blcokUserPan.add(blockuserbutton, blockuserCon);
                
                blockUserFrame.pack();
                blockUserFrame.setVisible(true);
                //client.handleMessageFromClientUI("#channel");
            
            
                
                
                
            }
        });
        blockButtons.add(innerBlockButton);
        JMenuItem unblockButton = new JMenuItem("Unblock");
        unblockButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                

                
                JFrame blockUserFrame = new JFrame("unblcok user");
                JPanel blcokUserPan = new JPanel();
                GridBagConstraints blockuserCon = new GridBagConstraints();
                
                
                JTextField blockusertext = new JTextField();
                blockusertext.setPreferredSize(new Dimension(400, 33));
                blockusertext.setBorder(currentChannel);
                blockuserCon.fill = GridBagConstraints.BOTH;
                blockuserCon.gridx = 0;
                blockuserCon.gridy = 1;
                blockuserCon.gridwidth = 1;
                blcokUserPan.add(blockusertext , blockuserCon);
                
                blockUserFrame.getContentPane().add(blcokUserPan);
                
                
                JButton blockuserbutton = new JButton("unblock");
                blockuserbutton.setFont(buttonFont);
                blockuserbutton.setPreferredSize(new Dimension(200,33));
                blockuserbutton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        
                        client.handleMessageFromClientUI("#unblock" + " " + blockusertext .getText());
                        blockUserFrame.dispose();
                        frame.setVisible(true);
                    }
                });
                blockuserCon.fill = GridBagConstraints.BOTH;
                blockuserCon.gridx = 1;
                blockuserCon.gridy = 1;
                blcokUserPan.add(blockuserbutton, blockuserCon);
                
                blockUserFrame.pack();
                blockUserFrame.setVisible(true);
                //client.handleMessageFromClientUI("#channel");
                
            }
        });
        blockButtons.add(unblockButton);
        JMenuItem whoIBlockButton = new JMenuItem("Who I Block");
        whoIBlockButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	client.handleMessageFromClientUI("#whoiblock");
            }
        });
        blockButtons.add(whoIBlockButton);
        JMenuItem whoBlocksMeButton = new JMenuItem("Who Blocks Me");
        whoBlocksMeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	client.handleMessageFromClientUI("#whoblocksme");
            }
        });
        blockButtons.add(whoBlocksMeButton);
        JButton blockButton = new JButton("Block");
        blockButton.setPreferredSize(new Dimension(150, 50));
        blockButton.setFont(buttonFont);
        blockButton.setComponentPopupMenu(blockButtons);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 1;
        buttonPanel.add(blockButton, constraints);
        
        // Private Message Button
        JButton privateMessageButton = new JButton("Private Message");
        privateMessageButton.setPreferredSize(new Dimension(150, 50));
        privateMessageButton.setFont(buttonFont);
        privateMessageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {    
                JFrame privMsg = new JFrame("Private Message");
                JPanel privPan = new JPanel();
                GridBagConstraints privcon = new GridBagConstraints();
                
                JLabel tolabel = new JLabel("To:");
                tolabel.setFont(new Font("Verdana",1,20));
                tolabel.setPreferredSize(new Dimension(60,33));
                privcon.fill = GridBagConstraints.BOTH;
                privcon.gridx = 0;
                privcon.gridy = 0;
                privcon.gridwidth = 2;
                privPan.add(tolabel, privcon);
                
                
                JTextField privusername = new JTextField();
                privusername.setPreferredSize(new Dimension(400, 33));
                privusername.setBorder(currentChannel);
                privcon.fill = GridBagConstraints.BOTH;
                privcon.gridx = 0;
                privcon.gridy = 0;
                privcon.gridwidth = 1;
                privPan.add(privusername, privcon);
                
                JLabel msglable = new JLabel("Message:");
                msglable.setFont(new Font("Verdana",1,20));
                msglable.setPreferredSize(new Dimension(60,33));
                privcon.fill = GridBagConstraints.BOTH;
                privcon.gridx = 0;
                privcon.gridy = 1;
                privcon.gridwidth = 2;
                privPan.add(msglable, privcon);
                
                JTextField priveusermsg = new JTextField();
                priveusermsg.setPreferredSize(new Dimension(400, 33));
                priveusermsg.setBorder(currentChannel);
                privcon.fill = GridBagConstraints.BOTH;
                privcon.gridx = 0;
                privcon.gridy = 1;
                privcon.gridwidth = 1;
                privPan.add(priveusermsg, privcon);
                
                JButton privSend = new JButton("Send Message");
                privSend.setFont(buttonFont);
                privSend.setPreferredSize(new Dimension(200,33));
                privSend.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        
                        client.handleMessageFromClientUI("#private" + " " + privusername.getText() + " " + priveusermsg.getText());;
                        privMsg.dispose();
                        frame.setVisible(true);
                    }
                });
                privcon.fill = GridBagConstraints.BOTH;
                privcon.gridx = 1;
                privcon.gridy = 1;
                privPan.add(privSend, privcon);
                
                privMsg.getContentPane().add(privPan);
                
                
                
                privMsg.pack();
                privMsg.setVisible(true);
                //client.handleMessageFromClientUI("#channel");
            
            
            }
        });
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 2;
        buttonPanel.add(privateMessageButton, constraints);
        frame.getContentPane().add(buttonPanel, BorderLayout.EAST);
        
        // DrawPad Button
            JButton drawPadButton = new JButton("DrawPad");
            drawPadButton.setPreferredSize(new Dimension(150, 50));
            drawPadButton.setFont(buttonFont);
            drawPadButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {    
                    StartDraw startDraw = new StartDraw();
                    openDP = new OpenDrawPad(startDraw,thisGUI);
                    //openDP.addObserver(thisGUI);    
                }
            });
            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridx = 0;
            constraints.gridy = 3;
            buttonPanel.add(drawPadButton, constraints);
            frame.getContentPane().add(buttonPanel, BorderLayout.EAST);
        
        
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * This method overrides the method in the ChatIF interface.  It
     * displays a message onto the screen.
     *
     * @param message The string to be displayed.
     */
    public void display(String message) 
    {
        String[] msg = ((String)message).split(" ");
        if(msg[0].equals("#send")){
            message = message.substring(6);
            openDP.update(null,message);
        }else{
            messageBox.append("> " + message + "\n");
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if(o instanceof OpenDrawPad){
            try {
                client.sendToServer(arg);
            } catch (IOException e) {}
        }
        //currentChannel.setTitle((String)arg);
    }

    //Class methods ***************************************************

    /**
     * This method is responsible for the creation of the Client UI.
     * @param args[0] The login name
     * @param args[1] The host to connect to.
     * @param args[2] The port to connect to.
     */
    public static void main(String[] args) 
    {
        String host = "";

        int port = 1;  //The port number
        //check to make sure there is a loginId...
        String loginID = "";
        try
        {
            loginID = args[0];
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            System.out.println("ERROR - No login ID specified.  Connection aborted.");
            System.exit(-1);
        }
        try
        {
            host = args[1];
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            host = "localhost";
        }
        try
        {
            port = Integer.parseInt(args[2]);
        }
        catch(Exception e) {
            port = DEFAULT_PORT;
        }
        ChatGUI chat= new ChatGUI(loginID ,host, port);
        chat.accept();  //Wait for console data
    }

}
//End of ChatGUI class