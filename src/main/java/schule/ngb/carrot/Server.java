package schule.ngb.carrot;

import schule.ngb.carrot.events.EventDispatcher;
import schule.ngb.carrot.events.ServerEvent;
import schule.ngb.carrot.events.ServerListener;
import schule.ngb.carrot.protocol.ProtocolHandler;
import schule.ngb.carrot.protocol.ProtocolHandlerFactory;
import schule.ngb.carrot.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server implements Runnable {

	private static final int SO_TIMEOUT = 500;

	private static final int TERMINATION_TIMEOUT = 3000;


	private static final Log LOG = Log.getLogger(Server.class);


	private int port;

	private boolean running = false;

	private int connectionTimeout = -1;

	private final Runnable timer;

	private final ProtocolHandlerFactory factory;

	private /*final*/ ExecutorService exec;

	private final ArrayList<ProtocolHandler> connections;

	private final EventDispatcher<ServerEvent, ServerListener> dispatcher;

	public Server( int port, ProtocolHandlerFactory phFactory ) {
		this.port = port;
		this.factory = phFactory;
		this.connections = new ArrayList<>();

		this.dispatcher = new EventDispatcher<>();
		this.dispatcher.registerEventType("started", ( e, l ) -> l.started(e));
		this.dispatcher.registerEventType("stopped", ( e, l ) -> l.stopped(e));
		this.dispatcher.registerEventType("connected", ( e, l ) -> l.clientConnected(e));
		this.dispatcher.registerEventType("disconnected", ( e, l ) -> l.clientDisconnected(e));
		this.dispatcher.registerEventType("timeout", ( e, l ) -> l.clientTimeout(e));

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

	public String getName() {
		return this.factory.getName();
	}

	public int getPort() {
		return port;
	}

	public void setPort( int port ) {
		if( !this.running ) {
			this.port = port;
		}
	}

	public boolean isRunning() {
		return running;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout( int connectionTimeout ) {
		if( !this.running ) {
			this.connectionTimeout = connectionTimeout;
		}
	}

	public void start() {
		// Initialize
		this.exec = Executors.newCachedThreadPool();
		this.connections.clear();

		Thread runner = new Thread(this);
		// Configure thread
		runner.start();
	}

	public void close() {
		this.running = false;
	}

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
			LOG.warn("Port number %d for server %s out of range", this.port, this.getName());
		} catch( IOException | InterruptedException ignored ) {
		}
	}


	public void disconnectAll() {
		for( ProtocolHandler ph : connections ) {
			ph.close();
		}
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
