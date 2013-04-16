import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.StringTokenizer;
/**
 * Open an SMTP connection to a mailserver and send one mail.
 *
 */
public class SMTPConnection {
	/* The socket to the server */
	private Socket connection;
	/* Streams for reading and writing the socket */
	private BufferedReader fromServer;
	private PrintStream pToServer;
	private DataOutputStream toServer;	
	private static final int SMTP_PORT = 25;
	private static final String CRLF = "\r\n";
	
	/* Are we connected? Used in close() to determine what to do. */
	private boolean isConnected = false;
	/* Create an SMTPConnection object. Create the socket and the
associated streams. Initialize SMTP connection. */
	
	public SMTPConnection(Envelope envelope) throws IOException {
		connection = new Socket(envelope.DestAddr, SMTP_PORT);
		fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream())); //TODO: change later
		toServer = new DataOutputStream(connection.getOutputStream());
		
		/* Read a line from server and check that the reply code is 220.
If not, throw an IOException. */
		String reply = fromServer.readLine();
		if (parseReply(reply) != 220)
			throw new IOException();
		
		/* SMTP handshake. We need the name of the local machine. Send the appropriate SMTP handshake command. */
		String localhost = InetAddress.getLocalHost().getHostName();
		sendCommand("HELO "+localhost+CRLF,250);
		isConnected = true;
	}
	/* Send the message. Write the correct SMTP-commands in the
correct order. No checking for errors, just throw them to the
caller. */
	public void send(Envelope envelope) throws IOException {
		/* Send all the necessary commands to send a message. Call sendCommand() to do the dirty work. Do _not_ catch the
      	exception thrown from sendCommand(). */
		// Sender info
		sendCommand("MAIL FROM:" + envelope.Sender + CRLF, 250);
		// Receiver info
		sendCommand("RCPT TO:" + envelope.Recipient + CRLF, 250);
		// Data
		sendCommand("DATA" + CRLF, 354);
		
		sendCommand(envelope.Message + CRLF + "." + CRLF, 250);

	}
	/* Close the connection. First, terminate on SMTP level, then
A Mail User Agent in Java http://media.pearsoncmg.com/aw/aw_kurose_network_3/labs/lab2/lab2.html
4 of 5 9/9/2010 11:26 AM
close the socket. */
	public void close(){
		isConnected = false;
		try {
			sendCommand("QUIT"+CRLF, 221);
			connection.close();
		} catch (IOException e) {
			System.out.println("Unable to close connection: " + e);
			isConnected = true;
		}
	}
	/* Send an SMTP command to the server. Check that the reply code is
what is is supposed to be according to RFC 821. */
	private void sendCommand(String command, int rc) throws IOException {
		/* Write command to server and read reply from server. */
		toServer.writeBytes(command);
		String reply = fromServer.readLine();

		/* Check that the server's reply code is the same as the parameter rc. If not, throw an IOException. */
		if (parseReply(reply) != rc) {
			throw new IOException();
		}
	}
	/* Parse the reply line from the server. Returns the reply code. */
	private int parseReply(String reply) {
		int code;
		code = -1;
		StringTokenizer lolreply = new StringTokenizer(reply);
		code = Integer.parseInt(lolreply.nextToken());
		return code;
	}
	/* Destructor. Closes the connection if something bad happens. */
	protected void finalize() throws Throwable {
		if(isConnected) {
			close();
		}
		super.finalize();
	}
}