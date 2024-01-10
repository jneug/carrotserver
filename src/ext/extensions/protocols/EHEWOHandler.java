import org.ini4j.Ini;
import schule.ngb.carrot.protocol.Protocol;
import schule.ngb.carrot.protocol.StringProtocolHandler;

import java.net.Socket;

@Protocol( name = "ehewo", port = 4445 )
public class EHEWOHandler extends StringProtocolHandler {

	public EHEWOHandler( Socket clientSocket, Ini config ) {
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
			send(config.get("ehewo", "reply"));
		}
	}

	@Override
	public void handleDisconnect() {
	}

}
