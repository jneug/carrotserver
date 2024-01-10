package schule.ngb.carrot.protocol;

import schule.ngb.carrot.util.Timer;

import java.net.Socket;

public interface ProtocolHandler extends Runnable {

	boolean isRunning();

	boolean isClosed();

	Timer getTimer();

	void stop();

	void close();

	Socket getSocket();

}
