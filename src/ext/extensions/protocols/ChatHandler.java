import schule.ngb.carrot.protocol.CommandProtocolHandler;
import schule.ngb.carrot.protocol.Protocol;
import schule.ngb.carrot.protocol.ProtocolException;
import schule.ngb.carrot.util.Configuration;

import java.net.Socket;

@Protocol( name = "chat", port = 6666, factory = ChatFactory.class )
public class ChatHandler extends CommandProtocolHandler {

	private final ChatFactory.Chatroom chatroom;

	private String nickname = null;

	public ChatHandler( Socket clientSocket, Configuration config, ChatFactory.Chatroom chatroom ) {
		super(clientSocket, config);
		this.chatroom = chatroom;
	}

	public String getNickname() {
		return nickname;
	}

	@Override
	public void handleConnect() {
		send("Welcome to the chat. Please set a nickname with NAME first.");
	}

	@Override
	public void handleCommand( String command, String value ) throws ProtocolException {
		if( command.equalsIgnoreCase("QUIT") ) {
			stop();
		} else if( command.equalsIgnoreCase("NAME") ) {
			if( nickname != null ) {
				throw new ProtocolException("You already set a nickname that can't be changed.");
			} else if( !chatroom.isNicknameFree(value) ) {
				throw new ProtocolException("Nickname already in use.");
			} else {
				nickname = value;
				sendOk("Hello %s", nickname);
				chatroom.broadcastOthers(this, nickname + " joined the chat");
			}
		} else {
			if( nickname == null ) {
				throw new ProtocolException("You need to set a nickname with NAME before sending messages.");
			} else {
				chatroom.broadcastOthers(this, this.message);
			}
		}
	}

	@Override
	public void handleDisconnect() {
		send("Goodbye. Hope you enjoyed your chat.");
		if( nickname != null ) {
			chatroom.broadcastOthers(this, nickname + " left the chat");
		}
	}

	public void sendOk( String message, String... args ) {
		send("+ %s", String.format(message, (Object[]) args));
	}

	public void sendErr( String message, String... args ) {
		send("- %s", String.format(message, (Object[]) args));
	}

	@Override
	public void sendError( ProtocolException e ) {
		sendErr(e.getMessage());
	}

}
