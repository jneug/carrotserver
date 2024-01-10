package schule.ngb.carrot.protocol;

import org.ini4j.Ini;
import schule.ngb.carrot.util.Log;

import java.net.Socket;

@Protocol( name = "echo", port = 7 )
public class EchoHandler extends StringProtocolHandler {

	// Logger
	private static final Log LOG = Log.getLogger(EchoHandler.class);


	/**
	 * Nachrichtenformat für echo Antworten.
	 */
	private final String messageFormat;

	/**
	 * Erstellt einen neuen {@code EchoHandler} für einen eingehende Verbindung.
	 *
	 * @param clientSocket
	 * @param config
	 */
	public EchoHandler( Socket clientSocket, Ini config ) {
		super(clientSocket, config);

		messageFormat = config.get("echo", "message_format");
	}

	@Override
	public void handleConnect() {
	}

	@Override
	public void handleMessage( String message ) {
		if( messageFormat != null ) {
			send(messageFormat, message);
		} else {
			send(message);
		}
	}

	@Override
	public void handleDisconnect() {
	}

}
