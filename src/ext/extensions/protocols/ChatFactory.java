import org.ini4j.Ini;
import schule.ngb.carrot.protocol.GenericProtocolHandlerFactory;
import schule.ngb.carrot.protocol.ProtocolHandler;

import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class ChatFactory extends GenericProtocolHandlerFactory {

	static final class Chatroom {

		List<ChatHandler> clients;

		public Chatroom() {
			this.clients = new LinkedList<>();
		}

		public void addClient( ChatHandler client ) {
			this.clients.add(client);
		}

		public void broadcast( ChatHandler source, String message ) {
			// Cleanup first
			clients.removeIf(ChatHandler::isClosed);
			// Broadcast
			for( ChatHandler client : clients ) {
				client.send(source.getNickname() + ": " + message);
			}
		}

		public void broadcastOthers( ChatHandler source, String message ) {
			// Cleanup first
			clients.removeIf(ChatHandler::isClosed);
			// Broadcast
			for( ChatHandler client : clients ) {
				if( !client.equals(source) ) {
					client.send(source.getNickname() + ": " + message);
				}
			}
		}

		public boolean isNicknameFree( String nickname ) {
			for( ChatHandler client : clients ) {
				if( nickname.equalsIgnoreCase(client.getNickname()) ) {
					return false;
				}
			}
			return true;
		}

	}


	private final Chatroom chatroom;

	public ChatFactory( Ini config ) {
		super(config, ChatHandler.class);
		chatroom = new Chatroom();
	}

	@Override
	public ProtocolHandler create( Socket clientSocket ) {
		ChatHandler client = new ChatHandler(clientSocket, config, chatroom);
		chatroom.addClient(client);
		return client;
	}

}
