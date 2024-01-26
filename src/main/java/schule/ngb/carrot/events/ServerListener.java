package schule.ngb.carrot.events;

import schule.ngb.carrot.Server;

/**
 * Ein Listener, der auf {@link ServerEvent}s hört, die von einem {@link Server} ausgelöst werden.
 */
public interface ServerListener extends Listener<ServerEvent> {

	/**
	 * Wird aufgerufen, sobald der Server {@link Server#start() gestartet} wurde.
	 *
	 * @param e Das Server-Event.
	 */
	void serverStarted( ServerEvent e );

	/**
	 * Wird aufgerufen, sobald der Server {@link Server#close() gestoppt} wurde.
	 *
	 * @param e Das Server-Event.
	 */
	void serverStopped( ServerEvent e );

	/**
	 * Wird aufgerufen, sobald ein neuer Client eine Verbindung zum Server aufbaut.
	 *
	 * @param e Das Server-Event.
	 */
	void clientConnected( ServerEvent e );

	/**
	 * Wird aufgerufen, sobald eine bestehende Verbindung zu einem Client unterbrochen wurde. Eine
	 * Unterbrechung kann sowohl ein aktiv vom Server oder Client ausgeführtes Schließen der
	 * Verbindung sein, als auch ein unvorhergesehene Unterbrechung.
	 *
	 * @param e Das Server-Event.
	 */
	void clientDisconnected( ServerEvent e );

	/**
	 * Wird aufgerufen, wenn eine bestehende Client-Verbindung aufgrund eines Server-Timeouts
	 * unterbrochen wurde.
	 *
	 * @param e Das Server-Event.
	 */
	void clientTimeout( ServerEvent e );

}
