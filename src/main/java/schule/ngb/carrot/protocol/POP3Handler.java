package schule.ngb.carrot.protocol;

import org.ini4j.Ini;
import schule.ngb.carrot.CarrotServer;
import schule.ngb.carrot.maildrop.FilesystemMaildrop;
import schule.ngb.carrot.maildrop.Mail;
import schule.ngb.carrot.maildrop.Maildrop;
import schule.ngb.carrot.maildrop.MaildropException;
import schule.ngb.carrot.util.Log;
import schule.ngb.carrot.util.Configuration;

import java.io.IOException;
import java.math.BigInteger;
import java.net.ProtocolException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;

@Protocol( name="pop3", port=110, factory=POP3Factory.class )
public class POP3Handler extends StringProtocolHandler {

	private static final Log LOG = Log.getLogger(POP3Handler.class);


	public static final int STATE_AUTHORIZATION = 0;
	public static final int STATE_TRANSACTION = 1;
	public static final int STATE_UPDATE = 2;
	public static final char TERMINATOR = '.';


	private int state = STATE_AUTHORIZATION;

	private String username;

	private Maildrop maildrop;

	private long timestamp;

	private final String[] capabilities;

	public POP3Handler( Socket clientSocket, Ini config ) {
		super(clientSocket, config);

		capabilities = Configuration.toArray(config.get("pop3", "capabilities"));
	}

	public boolean hasCapability( String capa ) {
		for( String c: capabilities ) {
			if( capa.equalsIgnoreCase(c) ) {
				return true;
			}
		}
		return false;
	}

	public boolean hasUser( String name ) {
		return config.get("users", name) != null;
	}

	public String getPassword( String name ) {
		return config.get("users", name);
	}

	private String getMessageId() {
		return String.format(
			"<%d@%s>",
			this.timestamp,
			this.config.get("host")
		);
	}

	private boolean canHash() {
		try {
			MessageDigest.getInstance("MD5");
			return true;
		} catch( NoSuchAlgorithmException ex ) {
			return false;
		}
	}

	private String getHash( String word ) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(StandardCharsets.UTF_8.encode(word));
			return String.format("%032x", new BigInteger(1, md5.digest()));
		} catch( NoSuchAlgorithmException ex ) {
			return null;
		}
	}

	@Override
	public void handleConnect() {
		// Timestamp for APOP authentication
		this.timestamp = System.currentTimeMillis();

		if( config.get("pop3", "enable_apop", boolean.class) && canHash() ) {
			sendOk("Welcome to %s (v%s) %s", CarrotServer.APP_NAME, CarrotServer.APP_VERSION, getMessageId());
		} else {
			sendOk("Welcome to %s (v%s)", CarrotServer.APP_NAME, CarrotServer.APP_VERSION);
		}
		LOG.info("POP3 client connected from %s", socket.getInetAddress());
	}

	@Override
	public void handleDisconnect() {
		if( socket != null && socket.isConnected() ) {
			LOG.info("POP3 client disconnected from %s", socket.getInetAddress());
		}

		try {
			if( maildrop != null ) {
				maildrop.unlock();
			}
		} catch( IOException ex ) {
			LOG.error(ex, "failed to unlock maildrop for user %s", username);
		}
	}

	@Override
	public void handleMessage( String message ) {
		String[] parts = splitMessage(message);
		String command = parts[0];
		String value = parts[1];

		resetTimer();

		try {
			if( command.equals("CAPA") && hasCapability("CAPA") ) {
				handleCapa();
			} else if( command.equals("USER") && hasCapability("USER") ) {
				handleUser(value);
			} else if( command.equals("PASS") && hasCapability("USER") ) {
				handlePass(value);
			} else if( command.equals("APOP") && hasCapability("APOP") ) {
				handleApop(value);
			} else if( command.equals("STAT") ) {
				handleStat();
			} else if( command.equals("LIST") ) {
				handleList(value);
			} else if( command.equals("RETR") ) {
				handleRetr(value);
			} else if( command.equals("TOP") && hasCapability("TOP") ) {
				handleTop(value);
			} else if( command.equals("UIDL") && hasCapability("UIDL") ) {
				handleUidl(value);
			} else if( command.equals("DELE") ) {
				handleDele(value);
			} else if( command.equals("RSET") ) {
				handleRset();
			} else if( command.equals("NOOP") ) {
				sendOk();
			} else if( command.equals("QUIT") ) {
				handleQuit();
			} else {
				sendErr("unknown command: %s", command);
			}
		} catch( ProtocolException pe ) {
			sendErr(pe.getMessage());
		}

	}

	private void checkState( int state ) throws ProtocolException {
		if( this.state != state ) {
			throw new ProtocolException("command invalid in this state");
		}
	}

	private void handleUser( String value ) throws ProtocolException {
		checkState(STATE_AUTHORIZATION);

		username = value;
		sendOk();
	}

	private void handlePass( String value ) throws ProtocolException {
		checkState(STATE_AUTHORIZATION);

		if( username == null ) {
			throw new ProtocolException("no username given");
		}

		if( hasUser(username) && getPassword(username).equals(value) ) {
			try {
				createMaildrop();

				state = STATE_TRANSACTION;
				sendOk("welcome %s", username);
			} catch( ProtocolException e ) {
				throw e;
			} catch( IOException ex ) {
				LOG.error(ex, "Could not initialize maildrop for user %s", username);
				throw new ProtocolException("operation failed with an error");
			}
		} else {
			username = null;
			throw new ProtocolException("authentication failed");
		}
	}

	private void handleApop( String value ) throws ProtocolException {
		checkState(STATE_AUTHORIZATION);

		String[] parts = value.split(" ");
		if( parts.length != 2 ) {
			throw new ProtocolException("wrong number of arguments");
		}

		username = parts[0];
		String userHash = parts[1];

		if( hasUser(username) ) {
			String pwdHash = getHash(getMessageId() + getPassword(username));
			if( userHash.equalsIgnoreCase(pwdHash) ) {
				try {
					createMaildrop();

					state = STATE_TRANSACTION;
					sendOk("welcome %s", username);
				} catch( ProtocolException ex ) {
					throw ex;
				} catch( IOException ex ) {
					LOG.error(ex, "Could not initialize maildrop for user %s", username);
					throw new ProtocolException("operation failed with an error");
				}
			} else {
				throw new ProtocolException("authentication failed");
			}
		} else {
			throw new ProtocolException("authentication failed");
		}
	}

	private void handleCapa() throws ProtocolException {
		sendOk("Listing capabilities");
		for( String capa : capabilities ) {
			if( !capa.equals("APOP") ) {
				send(capa);
			}
		}
		send(Character.toString(TERMINATOR));
	}

	private void createMaildrop() throws ProtocolException, IOException {
		maildrop = new FilesystemMaildrop(username, config);
		if( maildrop.isLocked() ) {
			maildrop = null;
			username = null;
			throw new ProtocolException("maildrop already in use");
		} else {
			maildrop.lock();
		}
	}

	private void handleRset() throws ProtocolException {
		checkState(STATE_TRANSACTION);

		maildrop.resetDeleted();
		sendOk("deleted mails restored");
	}

	private void handleStat() throws ProtocolException {
		checkState(STATE_TRANSACTION);

		try {
			sendOk("%d %d", maildrop.count(), maildrop.size());
		} catch( IOException ex ) {
			LOG.error(ex, "failed to get maildrop size for user %s", username);
			throw new ProtocolException("operation failed with an error");
		}
	}

	private void handleList( String value ) throws ProtocolException {
		checkState(STATE_TRANSACTION);

		try {
			if( value.isEmpty() ) {
				sendOk("%d %d", maildrop.count(), maildrop.size());
				for( Mail mail : maildrop.listMails() ) {
					send("%d %d", mail.getNumber(), mail.getSize());
				}
				send(Character.toString(TERMINATOR));
			} else {
				int number = Integer.parseInt(value);
				sendOk("%d %d", number, maildrop.size(number));
			}
		} catch( IOException ex ) {
			LOG.error(ex, "failed to get maildrop size for user %s", username);
			throw new ProtocolException("operation failed with an error");
		} catch( NumberFormatException ex ) {
			throw new ProtocolException("no such message");
		}
	}

	public void handleRetr( String value ) throws ProtocolException {
		checkState(STATE_TRANSACTION);

		if( value.isEmpty() ) {
			throw new ProtocolException("missing argument");
		}

		try {
			int number = Integer.parseInt(value);

			try {
				List<String> lines = maildrop.getLines(number);
				sendOk("%d octets", maildrop.size(number));
				for( String line : lines ) {
					if( !line.isEmpty() && line.charAt(0) == TERMINATOR ) {
						line = TERMINATOR + line;
					}
					send(line);
				}
				send(Character.toString(TERMINATOR));
			} catch( NumberFormatException |
					 MaildropException ex ) {
				throw new ProtocolException("no such message");
			}
		} catch( IOException ex ) {
			LOG.error(ex, "failed to read maildrop for user ", username);
			throw new ProtocolException("operation failed with an error");
		}
	}

	public void handleTop( String value ) throws ProtocolException {
		checkState(STATE_TRANSACTION);

		String[] parts = value.split(" ");
		if( parts.length != 2 ) {
			throw new ProtocolException("wrong number of arguments");
		}

		try {
			int number = Integer.parseInt(parts[0]);
			int n = Integer.parseInt(parts[1]);

			List<String> lines = maildrop.getLines(number);
			sendOk("top of message follows");

			// Send headers
			Iterator<String> it = lines.iterator();
			while( it.hasNext() ) {
				String line = it.next();
				send(line);

				if( line.isEmpty() ) {
					break;
				}
			}

			while( it.hasNext() && n > 0 ) {
				String line = it.next();
				if( !line.isEmpty() && line.charAt(0) == TERMINATOR ) {
					line = TERMINATOR + line;
				}

				send(line);
				n -= 1;
			}

			send(Character.toString(TERMINATOR));
		} catch( NumberFormatException |
				 MaildropException ex ) {
			throw new ProtocolException("no such message");
		}
	}

	private void handleDele( String value ) throws ProtocolException {
		checkState(STATE_TRANSACTION);

		if( !value.isEmpty() ) {
			try {
				int number = Integer.parseInt(value);

				maildrop.deleteFile(number);
				LOG.debug("user %s marked mail %d for deletion", username, number);
				sendOk("mail marked for deletion");
			} catch( NumberFormatException | MaildropException ex ) {
				throw new ProtocolException("no such message");
			}
		} else {
			throw new ProtocolException("missing argument");
		}
	}


	private void handleUidl( String value ) throws ProtocolException {
		checkState(STATE_TRANSACTION);

		try {
			if( value.isEmpty() ) {
				sendOk();
				for( Mail mail : maildrop.listMails() ) {
					if( config.get("pop3", "uidl_hash", boolean.class) ) {
						send("%d %s", mail.getNumber(), mail.getHash());
					} else {
						send("%d %s", mail.getNumber(), mail.getId());
					}
				}
				send(Character.toString(TERMINATOR));
			} else {
				int number = Integer.parseInt(value);
				sendOk("%d %d", number, maildrop.getMail(number).getId());
			}
		} catch( IOException ex ) {
			LOG.error(ex, "failed to get maildrop size for user %s", username);
			throw new ProtocolException("operation failed with an error");
		} catch( NumberFormatException ex ) {
			throw new ProtocolException("no such message");
		}
	}

	private void handleQuit() throws ProtocolException {
		state = STATE_UPDATE;
		try {
			if( maildrop != null ) {
				maildrop.executeDelete();
				maildrop.unlock();
			}
			sendOk("bye");
		} catch( IOException ex ) {
			LOG.error(ex, "failed to move mail files to trash for user %s", username);
			sendErr("failed to delete some mails");
		} finally {
			close();
		}
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

	private void sendOk() {
		send("+OK");
	}

	private void sendOk( String message ) {
		send("+OK %s", message);
	}

	private void sendOk( String message, Object... args ) {
		sendOk(String.format(message, args));
	}

	private void sendErr() {
		send("-ERR");
	}

	private void sendErr( String message ) {
		send("-ERR " + message);
	}

	private void sendErr( String message, Object... args ) {
		sendOk(String.format(message, args));
	}

}
