package schule.ngb.carrot.protocol;

import schule.ngb.carrot.util.Configuration;

import java.net.Socket;

public abstract class CommandProtocolHandler extends StringProtocolHandler {

	public static final String DEFAULT_SEPARATOR = " ";



	private String separator = DEFAULT_SEPARATOR;

	public CommandProtocolHandler( Socket clientSocket, Configuration config ) {
		super(clientSocket, config);
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator( String pSeparator ) {
		this.separator = pSeparator;
	}

	protected String[] splitMessage( String message ) {
		return message.split(DEFAULT_SEPARATOR, 1);
	}

	@Override
	public void handleMessage( String message ) {
		String[] parts = splitMessage(message);
		handleCommand(parts[0], parts[1]);
	}

	public abstract void handleCommand( String command, String value );

}
