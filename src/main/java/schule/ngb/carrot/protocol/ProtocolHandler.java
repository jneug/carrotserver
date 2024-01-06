package schule.ngb.carrot.protocol;

import schule.ngb.carrot.util.Timer;

public interface ProtocolHandler extends Runnable {

	boolean isRunning();

	boolean isClosed();

	Timer getTimer();

	void stop();

	void close();

}
