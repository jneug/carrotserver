import java.net.Socket;

import schule.ngb.carrot.protocol.Protocol;
import schule.ngb.carrot.protocol.StringProtocolHandler;
import schule.ngb.carrot.util.Configuration;

@Protocol
public class HEWOHandler extends StringProtocolHandler {
	public HEWOHandler( Socket clientSocket, Configuration config ) {
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
