package schule.ngb.carrot.protocol;

import org.ini4j.Ini;
import schule.ngb.carrot.util.Configuration;
import schule.ngb.carrot.util.Timer;

import java.io.IOException;
import java.net.Socket;

public abstract class GenericProtocolHandler implements ProtocolHandler {

	protected final Ini config;

	protected final Socket socket;

	protected boolean running = false;

	private final Timer timer;

	public GenericProtocolHandler( Socket clientSocket, Ini config ) {
		this.socket = clientSocket;
		this.config = config;

		this.timer = new Timer();
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public boolean isClosed() {
		return this.socket.isClosed() || !this.socket.isConnected();
	}

	@Override
	public Timer getTimer() {
		return timer;
	}

	public void resetTimer() {
		timer.reset();
	}

	protected void startTimer() {
		timer.start();
	}

	@Override
	public Socket getSocket() {
		return socket;
	}

	@Override
	public void stop() {
		running = false;
	}


	@Override
	public void close() {
		if( running ) {
			running = false;
			try {
				socket.shutdownInput();
				socket.shutdownOutput();
			} catch( IOException ignored ) {
			}
		}

		// Close socket
		if( socket != null ) {
			try {
				socket.close();
			} catch( IOException ignored ) {
			}
		}
	}

}
