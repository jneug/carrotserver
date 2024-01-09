package schule.ngb.carrot.protocol;

import org.ini4j.Ini;
import schule.ngb.carrot.util.Configuration;
import schule.ngb.carrot.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Implementierung eines Textbasierten-Protokolls wie POP3 oder SMTP.
 * <p>
 * Ein {@link StringProtocolHandler} kpmmert sich um die Verwaltung eines {@link Socket}s zu einem
 * Client, öffnet Streams für die {@link BufferedReader Eingabe} und {@link PrintWriter Ausgabe} und
 * sendet und liest ASCII-Daten Zeilenweise.
 */
public abstract class StringProtocolHandler extends GenericProtocolHandler {

	public static final String CRLF = "\r\n";

	private static final Log LOG = Log.getLogger(StringProtocolHandler.class);


	private BufferedReader in;

	private PrintWriter out;

	private boolean autoResetTimer = true;

	public StringProtocolHandler( Socket clientSocket, Ini config ) {
		super(clientSocket, config);

		try {
			out = new PrintWriter(clientSocket.getOutputStream());
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch( IOException e ) {
			close();
		}
	}

	public boolean isAutoResetTimer() {
		return autoResetTimer;
	}

	public void setAutoResetTimer( boolean pAutoResetTimer ) {
		this.autoResetTimer = pAutoResetTimer;
	}

	@Override
	public void run() {
		this.running = true;
		startTimer();

		if( !isClosed() ) {
			handleConnect();
		}

		String message;
		while( isRunning() ) {
			try {
				// Wait for the next full line of content
				message = in.readLine();
				if( message != null ) {
					LOG.debug("%s received: %s", this.getClass().getSimpleName(), message);
					if( autoResetTimer ) {
						resetTimer();
					}
					handleMessage(message);
				} else {
					// Connection was closed
					this.running = false;
				}
			} catch( IOException ex ) {
				// Connection failed or was closed
				this.running = false;
			}
		}

		// Handle protocol related shutdown before disconnecting streams and socket
		if( !isClosed() ) {
			handleDisconnect();
		}

		close();
	}

	@Override
	public void close() {
		super.close();

		// Close streams
		if( in != null ) {
			try {
				in.close();
			} catch( IOException ignored ) {
			} finally {
				in = null;
			}
		}
		if( out != null ) {
			out.close();
			out = null;
		}
	}

	public void send( String message ) {
		if( isRunning() ) {
			out.print(message);
			out.print(CRLF);
			out.flush();
		}
	}

	public void send( String message, Object... args ) {
		if( isRunning() ) {
			out.print(String.format(message, args));
			out.print(CRLF);
			out.flush();
		}
	}

	public void sendText( String message ) {
		for( String line : message.split(CRLF) ) {
			send(line);
		}
	}

	public abstract void handleConnect();

	public abstract void handleMessage( String message );

	public abstract void handleDisconnect();

}
