package schule.ngb.carrot;

import schule.ngb.carrot.events.EventDispatcher;
import schule.ngb.carrot.events.ServerEvent;
import schule.ngb.carrot.events.ServerListener;
import schule.ngb.carrot.protocol.ProtocolHandler;
import schule.ngb.carrot.protocol.ProtocolHandlerFactory;
import schule.ngb.carrot.util.Log;
import schule.ngb.carrot.util.Timer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Die Hauptklasse für einen Server, der über einen {@link ServerSocket} auf Verbindungen wartet und
 * für diese einen neuen {@link ProtocolHandler} instanziiert.
 * <p>
 * Jeder Client wird in einem eigenen Thread abgearbeitet. Der zugehörige {@code ProtocolHandler}
 * wird mit der zugewiesenen {@link ProtocolHandlerFactory} erstellt.
 * <p>
 * Falls ein {@link #setConnectionTimeout(int) Timeout} eingestellt ist, werden bestehende
 * Verbindungen automatisch getrennt, wenn vor Ablauf des Timeouts keine Befehle empfangen wurden.
 * Dazu muss jeder {@code ProtocolHandler} einen internen {@link Timer} verwalten und bei
 * eingehenden Befehlen {@link Timer#reset() zurücksetzen}. Anhand des Zustands des Timers
 * entscheidet der Server, ob die Verbindung getrennt wird. (Die Prüfung der Timeouts findet in
 * einem eigenen Thread statt und wird nicht durch das Warten auf eingehende Verbindungen
 * blockiert.)
 * <p>
 * Events wie der Start ud Stopp des Servers, die Verbindung von Clients und Trennung von
 * Verbindungen durch Timeout oder regulär können von {@link ServerListener}n abonniert werden.
 */
public class Server implements Runnable {

	/**
	 * Socket timeout (in ms), wie lange jeweils auf eingehende Verbindungen gewartet wird. Je höher
	 * der Wert, desto länger braucht der Server, um vollständig zu stoppen.
	 */
	private static final int SO_TIMEOUT = 500;

	/**
	 * Zeitverzögerung (in ms) beim Stoppen des Servers. So lange wird gewartet, bis alle
	 * bestehenden Verbindungen geschlossen wurden. Spätestens danach wird der Socket geschlossen.
	 */
	private static final int TERMINATION_TIMEOUT = 3000;


	// Logger
	private static final Log LOG = Log.getLogger(Server.class);


	/**
	 * Port, auf dem der Server auf Verbindungen wartet.
	 */
	private int port;

	/**
	 * Ob der Server gerade läuft und Verbindungen entgegen nimmt.
	 */
	private boolean running = false;

	/**
	 * Timeout für Verbindungen.
	 */
	private int connectionTimeout = -1;

	/**
	 * Timer-Thread für den Timeout.
	 */
	private final Runnable timer;

	/**
	 * Factory für {@link ProtocolHandler} dieses Servers.
	 */
	private final ProtocolHandlerFactory factory;

	/**
	 * Executor, auf dem die {@link ProtocolHandler} ausgeführt werden.
	 */
	private /*final*/ ExecutorService exec;

	/**
	 * Liste der Verbindungen. Nach jedem {@link #SO_TIMEOUT} wird die List bereinigt und
	 * geschlossene Verbindungen entfernt.
	 */
	private final ArrayList<ProtocolHandler> connections;

	/**
	 * Dispatcher für Server-Events.
	 */
	private final EventDispatcher<ServerEvent, ServerListener> dispatcher;


	/**
	 * Erstellt einen Server auf dem angegebenen Port für das angegebenen Protokoll.
	 * <p>
	 * ist der {@code port = 0}, dann wird ein zufälliger, freier Port gewählt.
	 *
	 * @param port Port, auf dem der Server Verbindungen entgegennimmt.
	 * @param phFactory Factory für die {@link ProtocolHandler}.
	 */
	public Server( int port, ProtocolHandlerFactory phFactory ) {
		this.port = port;
		this.factory = phFactory;
		this.connections = new ArrayList<>();

		// Events vorbereiten
		this.dispatcher = new EventDispatcher<>();
		this.dispatcher.registerEventType("started", ( e, l ) -> l.started(e));
		this.dispatcher.registerEventType("stopped", ( e, l ) -> l.stopped(e));
		this.dispatcher.registerEventType("connected", ( e, l ) -> l.clientConnected(e));
		this.dispatcher.registerEventType("disconnected", ( e, l ) -> l.clientDisconnected(e));
		this.dispatcher.registerEventType("timeout", ( e, l ) -> l.clientTimeout(e));

		// Timeout-Thread vorbereiten
		timer = new Runnable() {
			@Override
			public void run() {
				while( running ) {
					for( ProtocolHandler ph : connections ) {
						if( ph.isRunning() ) {
							if( ph.getTimer().getMillis() > connectionTimeout ) {
								ph.close();
								dispatch("timeout", ph);
							}
						}
					}

					try {
						Thread.sleep(1);
					} catch( InterruptedException ignored ) {
					}
				}
			}
		};
	}

	/**
	 * Liefert den Namen des Protokolle, das auf diesem Server läuft.
	 *
	 * @return
	 */
	public String getProtocolName() {
		return this.factory.getName();
	}

	/**
	 * Liefert den Port des Servers.
	 *
	 * @return Der Port, auf dem der Server läuft.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Setzt den Port des Servers.
	 * <p>
	 * Wenn der Server {@link #isRunning() läuft}, wird der Aufruf ignoriert.
	 *
	 * @param port Der neue Port des Servers.
	 */
	public void setPort( int port ) {
		if( !this.running ) {
			this.port = port;
		}
	}

	/**
	 * Liefert, ob der Server derzeit läuft und auf Verbindungsanfragen reagiert.
	 *
	 * @return {@code true}, wenn der Server-Thread läuft.
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Liefert den derzeit eingestellten Verbindungstimeout.
	 *
	 * @return Der Timeout für Verbindungen in Millisekunden.
	 */
	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * Setzt den Verbindungstimeout des Servers.
	 * <p>
	 * Wenn der Server {@link #isRunning() läuft}, wird der Aufruf ignoriert.
	 *
	 * @param connectionTimeout Der neue Verbindungstimeout.
	 */
	public void setConnectionTimeout( int connectionTimeout ) {
		if( !this.running ) {
			this.connectionTimeout = connectionTimeout;
		}
	}

	/**
	 * Startet den Server, sofern er nicht schon läuft.
	 */
	public void start() {
		if( !isRunning() ) {
			// Initialize
			this.exec = Executors.newCachedThreadPool();
			this.connections.clear();

			Thread runner = new Thread(this);
			// Configure thread
			runner.start();
		}
	}

	/**
	 * Stoppt den Server und trennt alle bestehenden Verbindungen. Der Server kann danach wieder
	 * {@link #start() neugestartet} werden.
	 */
	public void close() {
		this.running = false;
	}

	@Override
	public void run() {
		try( final ServerSocket serverSocket = new ServerSocket(this.port) ) {
			serverSocket.setSoTimeout(SO_TIMEOUT);
			// Get the actual port, the server runs on
			this.port = serverSocket.getLocalPort();

			running = true;
			// Start timeout thread
			if( connectionTimeout > 0 ) {
				new Thread(timer).start();
			}

			dispatch("started");
			while( running && !Thread.interrupted() ) {
				try {
					// Warten auf Verbindungsversuch durch Client:
					Socket clientSocket = serverSocket.accept();

					// Eingehende Nachrichten vom neu verbundenen Client werden
					// in einem eigenen Thread empfangen:

					ProtocolHandler ph = this.factory.create(clientSocket);
					this.connections.add(ph);
					this.exec.submit(ph);

					dispatch("connected", ph);
				} catch( SocketTimeoutException ex ) {
				}

				// Cleanup old connections
				// connections.removeIf(ProtocolHandler::isClosed);
				Iterator<ProtocolHandler> it = connections.iterator();
				while( it.hasNext() ) {
					ProtocolHandler ph = it.next();
					if( ph.isClosed() ) {
						it.remove();
						dispatch("disconnected", ph);
					}
				}
			}

			// Close all open connections ..
			disconnectAll();
			// .. and shutdown threads.
			exec.shutdownNow();
			exec.awaitTermination(TERMINATION_TIMEOUT, TimeUnit.MILLISECONDS);

			dispatch("stopped");
		} catch( IllegalArgumentException e ) {
			LOG.warn("Port number %d for server %s out of range", this.port, this.getProtocolName());
		} catch( IOException | InterruptedException ignored ) {
		}
	}

	/**
	 * Trennt alle bestehenden Verbindungen ohne den Server zu stoppen.
	 */
	public void disconnectAll() {
		Iterator<ProtocolHandler> it = connections.iterator();
		while( it.hasNext() ) {
			ProtocolHandler ph = it.next();
			ph.close();
			it.remove();
		}
//		for( ProtocolHandler ph : connections ) {
//			ph.close();
//		}
	}


	public void addListener( ServerListener listener ) {
		dispatcher.addListener(listener);
	}

	public void removeListener( ServerListener listener ) {
		dispatcher.removeListener(listener);
	}

	private void dispatch( String type ) {
		dispatch(type, null);
	}

	private void dispatch( String type, ProtocolHandler client ) {
		dispatcher.dispatchEvent(type, new ServerEvent(this, client, factory));
	}

}
