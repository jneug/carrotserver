package schule.ngb.carrot.protocol;

import org.ini4j.Ini;
import schule.ngb.carrot.CarrotServer;
import schule.ngb.carrot.maildrop.MailAddress;
import schule.ngb.carrot.protocol.SMTPFactory.TransmissionQueue;
import schule.ngb.carrot.util.Log;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Protocol( name = "smtp", port = 25, factory = SMTPFactory.class )
public class SMTPHandler extends StringProtocolHandler {

	private static final Log LOG = Log.getLogger(SMTPHandler.class);


	public static final int STATUS_SUCCESS = 200;

	public static final int STATUS_SYSTEM = 211;

	public static final int STATUS_HELP = 214;

	public static final int STATUS_READY = 220;

	public static final int STATUS_QUIT = 221;

	public static final int STATUS_OK = 250;

	public static final int STATUS_INTERMEDIATE_REPLY = 354;

	public static final int STATUS_DISCONNECT = 421;

	public static final int STATUS_LOCAL_ERR = 451;

	public static final int STATUS_UNKNOWN_CMD = 500;

	public static final int STATUS_UNKNOWN_ARG = 501;

	public static final int STATUS_WRONG_ORDER = 503;

	public static final int STATUS_NO_RECV = 521;

	public static final int STATUS_NO_ACCESS = 530;

	public static final int STATUS_FAILURE = 550;


	public static final int STATE_AUTHENTICATE = 0;

	public static final int STATE_INIT = 1;

	public static final int STATE_RCPT = 2;

	public static final int STATE_DATA = 3;

	public static final int STATE_QUIT = 10;

	public static final String TERMINATOR = ".";


	private int state = STATE_INIT, lastCode = 0;

	private String hostname;

	private MailAddress from;

	private List<MailAddress> recipients;

	private StringBuilder data;

	private TransmissionQueue transmissionQueue;

	public SMTPHandler( Socket clientSocket, Ini config, TransmissionQueue transmissionQueue ) {
		super(clientSocket, config);
		this.transmissionQueue = transmissionQueue;
	}

	private boolean isLocalMailPath( MailAddress mail ) {
		if( mail != null ) {
			String hostname = mail.getHostname();
			return hostname.equals("[127.0.0.1]")
				|| hostname.equals(String.format("[%s]", socket.getLocalAddress().getHostAddress()))
				|| hostname.equalsIgnoreCase(config.get("carrot", "host"));
		} else {
			return false;
		}
	}

	@Override
	public void handleConnect() {
		send(STATUS_READY, "%s SMTP ready on %s (v%s)",
			config.get("carrot", "host"),
			CarrotServer.APP_NAME, CarrotServer.APP_VERSION
		);
	}

	@Override
	public void handleDisconnect() {
		if( socket != null && socket.isConnected() ) {
			if( state != STATE_QUIT ) {
				send(STATUS_DISCONNECT, "connection interrupted, closing down");
			}
		}
	}

	@Override
	public void handleMessage( String message ) {
		resetTimer();

		if( state == STATE_DATA ) {
			handleData(message);
			return;
		}

		String[] parts = splitMessage(message);
		String command = parts[0];
		String value = parts[1];

		try {
			switch( command ) {

				case "EHLO":
				case "HELO":
					handleHelo(value);
					break;

				case "MAIL":
					handleMail(value);
					break;

				case "RCPT":
					handleRcpt(value);
					break;

				case "DATA":
					handleData(value);
					break;

				case "RSET":
					handleRset();
					break;

				case "NOOP":
					send(STATUS_OK, "OK");
					break;

				case "QUIT":
					handleQuit();
					break;

				/* not required for minimal implementation
				case "SEND":
					send(STATUS_LOCAL_ERR, "command not yet implemented");
					break;

				case "SOML":
					send(STATUS_LOCAL_ERR, "command not yet implemented");
					break;

				case "VRFY":
					send(STATUS_LOCAL_ERR, "command not yet implemented");
					break;

				case "EXPN":
					send(STATUS_LOCAL_ERR, "command not yet implemented");
					break;

				case "HELP":
					send(STATUS_LOCAL_ERR, "command not yet implemented");
					break;

				case "TURN":
					send(STATUS_LOCAL_ERR, "command not yet implemented");
					break;
				*/

				// SMTP AUTH Extension (move to ESMTPHandler)
				/*
				case "EHLO":
					handleEhlo(value);
					break;

				case "AUTH":
					handleAuth(value);
					break;
				 */

				default:
					send(STATUS_UNKNOWN_CMD, "Unknown command");
					break;
			}
		} catch( ProtocolException e ) {
			send(e.getCode(), e.getMessage());
		}
	}

	private void handleHelo( String value ) throws ProtocolException {
		hostname = value;
		send(STATUS_OK, config.get("carrot", "host"));
	}

	private void handleEhlo( String value ) throws ProtocolException {
		hostname = value;
		sendInline(STATUS_OK, config.get("carrot", "host"));
		send(STATUS_OK, "AUTH PLAIN DIGEST-MD5");
	}

	// TODO implement
	private void handleAuth( String value ) {
		if( value.toUpperCase().startsWith("PLAIN ") ) {
			String authData = new String(Base64.getDecoder().decode(value.substring(6)));
			String[] parts = authData.split("\0");
			LOG.info("%s", Arrays.toString(parts));
		}
	}

	private void handleMail( String value ) throws ProtocolException {
		if( !value.toUpperCase().startsWith("FROM:") ) {
			throw new ProtocolException(STATUS_FAILURE, "malformed command");
		}

		String mailPath = value.substring(5);
		if( mailPath.isBlank() ) {
			throw new ProtocolException(STATUS_UNKNOWN_ARG, "malformed <reverse-path>");
		}

		from = MailAddress.parseString(value);
		if( !from.isFullyQualified() ) {
			from = null;
			throw new ProtocolException(STATUS_UNKNOWN_ARG, "malformed <reverse-path>");
		}


		recipients = new ArrayList<>();
		data = new StringBuilder();

		state = STATE_RCPT;

		send(STATUS_OK, "OK");
	}

	private void handleRcpt( String value ) throws ProtocolException {
		if( state != STATE_RCPT ) {
			throw new ProtocolException(STATUS_WRONG_ORDER, "no source mailbox given yet");
		}

		if( !value.toUpperCase().startsWith("TO:") ) {
			throw new ProtocolException(STATUS_FAILURE, "malformed command");
		}

		MailAddress rcptAddr = MailAddress.parseString(value.substring(3));
		if( config.get("smtp", "accept_any_rcpt", boolean.class)
			|| (isLocalMailPath(rcptAddr)
			&& config.get("users", rcptAddr.getMailbox()) != null) ) {
			recipients.add(rcptAddr);
			send(STATUS_OK, "OK");
		} else {
			throw new ProtocolException(STATUS_FAILURE, "Unknown recipient");
		}
	}

	private void handleData( String value ) {
		if( state != STATE_DATA ) {
			state = STATE_DATA;
			send(STATUS_INTERMEDIATE_REPLY, "Ready to receive data; end with <CRLF>.<CRLF>");
		} else if( TERMINATOR.equals(value) ) {
			state = STATE_RCPT;

			int queue = transmissionQueue.queueTransmission(from, recipients, data.toString());
			send(STATUS_OK, "mail queued for transmission at %d", queue);
		} else {
			if( value.startsWith(TERMINATOR) ) {
				value = value.substring(1);
			}
			data.append(value);
			data.append(CRLF);
		}
	}

	private void handleRset() {
		state = STATE_INIT;
		from = null;
		recipients = null;
		data = null;
		send(STATUS_OK, "OK");
	}

	private void handleQuit() throws ProtocolException {
		state = STATE_QUIT;
		send(STATUS_QUIT, "bye");
		close();
	}

	private String[] splitMessage( String message ) {
		int space = message.indexOf(" ");
		if( space > 0 ) {
			return new String[]{
				message.substring(0, space).toUpperCase(),
				message.substring(space + 1)
			};
		} else {
			return new String[]{
				message.toUpperCase(),
				""
			};
		}
	}

	public void send( int code, String message ) {
		send("%d %s", code, message);
	}

	public void send( int code, String message, Object... args ) {
		send("%d %s", code, String.format(message, args));
	}

	public void sendInline( int code, String message, Object... args ) {
		send("%d-%s", code, String.format(message, args));
	}

}
