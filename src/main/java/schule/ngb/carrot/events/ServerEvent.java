package schule.ngb.carrot.events;

import schule.ngb.carrot.Server;
import schule.ngb.carrot.protocol.ProtocolHandler;
import schule.ngb.carrot.protocol.ProtocolHandlerFactory;

public final class ServerEvent {

	public final Server server;

	public final ProtocolHandler clientHandler;

	public final ProtocolHandlerFactory handlerFactory;

	public ServerEvent( Server server, ProtocolHandler clientHandler, ProtocolHandlerFactory handlerFactory ) {
		this.server = server;
		this.clientHandler = clientHandler;
		this.handlerFactory = handlerFactory;
	}

}
