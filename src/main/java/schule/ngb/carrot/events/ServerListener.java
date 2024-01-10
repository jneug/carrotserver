package schule.ngb.carrot.events;

public interface ServerListener extends Listener<ServerEvent> {

	void serverStarted( ServerEvent e );

	void serverStopped( ServerEvent e );

	void clientConnected( ServerEvent e );

	void clientDisconnected( ServerEvent e );

	void clientTimeout( ServerEvent e );

}
