import org.ini4j.Ini;
import schule.ngb.carrot.protocol.Protocol;
import schule.ngb.carrot.protocol.StringProtocolHandler;

import java.net.Socket;

@Protocol
public class HEWOHandler extends StringProtocolHandler {

	public HEWOHandler( Socket clientSocket, Ini config ) {
		super(clientSocket, config);
	}

	@Override
	public void handleConnect() {
	}

	@Override
	public void handleMessage( String message ) {
		if( message.equalsIgnoreCase("QUIT") ) {
			close();
		} else {
			send("Hello, World!");
		}
	}

	@Override
	public void handleDisconnect() {
	}

}
