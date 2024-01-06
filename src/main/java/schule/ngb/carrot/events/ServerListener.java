package schule.ngb.carrot.events;

public interface ServerListener extends Listener<ServerEvent> {

	void started( ServerEvent e );

	void stopped( ServerEvent e );

	void clientConnected( ServerEvent e );

	void clientDisconnected( ServerEvent e );

	void clientTimeout( ServerEvent e );

}
