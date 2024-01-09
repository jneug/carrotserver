package schule.ngb.carrot.protocol;

import org.ini4j.Ini;
import schule.ngb.carrot.util.Configuration;

import java.net.Socket;

public abstract class CommandProtocolHandler extends StringProtocolHandler {

	public static final String DEFAULT_SEPARATOR = " ";


	private String separator = DEFAULT_SEPARATOR;

	protected boolean parseCommand = true;

	protected String message = null;

	public CommandProtocolHandler( Socket clientSocket, Ini config ) {
		super(clientSocket, config);
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator( String pSeparator ) {
		this.separator = pSeparator;
	}

	public void enableCommandParsing() {
		this.parseCommand = true;
	}

	public void disableCommandParsing() {
		this.parseCommand = false;
	}

	public String getLastMessage() {
		return this.message;
	}

	protected String[] splitMessage( String message ) {
		String[] parts = message.split(DEFAULT_SEPARATOR, 2);
		if( parts.length == 1 ) {
			return new String[]{parts[0].trim(), ""};
		} else {
			return new String[]{parts[0].trim(), parts[1].trim()};
		}
	}

	@Override
	public void handleMessage( String message ) {
		this.message = message;

		String[] parts = null;
		if( parseCommand ) {
			parts = splitMessage(message);
		} else {
			parts = new String[]{"", message};
		}

		try {
			handleCommand(parts[0], parts[1]);
		} catch( ProtocolException e ) {
			sendError(e);
		}
	}

	public abstract void handleCommand( String command, String value ) throws ProtocolException;

	public void sendError( ProtocolException e ) {
		send(e.toString());
	}

}
