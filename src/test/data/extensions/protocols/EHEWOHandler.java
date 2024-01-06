import java.net.Socket;

import schule.ngb.carrot.protocol.Protocol;
import schule.ngb.carrot.protocol.StringProtocolHandler;
import schule.ngb.carrot.util.Configuration;

@Protocol( name = "ehewo", port = 4445 )
public class EHEWOHandler extends StringProtocolHandler {

	public EHEWOHandler( Socket clientSocket, Configuration config ) {
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
			send(config.getString("REPLY"));
		}
	}

	@Override
	public void handleDisconnect() {
	}

}
