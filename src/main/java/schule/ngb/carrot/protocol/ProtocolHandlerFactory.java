package schule.ngb.carrot.protocol;

import java.net.Socket;

public interface ProtocolHandlerFactory {

	ProtocolHandler create( Socket clientSocket );

	String getName();

	int getPort();

}
