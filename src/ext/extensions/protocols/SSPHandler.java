import org.ini4j.Ini;
import schule.ngb.carrot.protocol.CommandProtocolHandler;
import schule.ngb.carrot.protocol.Protocol;
import schule.ngb.carrot.protocol.ProtocolException;
import schule.ngb.carrot.util.Configuration;
import schule.ngb.carrot.util.Log;

import java.net.Socket;
import java.util.Random;

@Protocol( name = "ssp", port = 1300 )
public class SSPHandler extends CommandProtocolHandler {

	private static final Log LOG = Log.getLogger(SSPHandler.class);


	private static final int SYMBOL_UNKNOWN = -1;

	private static final int SYMBOL_STONE = 1;

	private static final int SYMBOL_SCISSORS = 2;

	private static final int SYMBOL_PAPER = 3;

	public static final int STATE_INITIALIZING = 0;

	public static final int STATE_PLAY = 1;

	public static final int STATE_QUIT = 2;


	private int state = STATE_INITIALIZING;

	private int roundsTotal, roundsCurrent, winsTotal;

	private final String[] stone, scissors, paper;

	private final Random rand;

	public SSPHandler( Socket clientSocket, Ini config ) {
		super(clientSocket, config);

		rand = new Random();

		stone = Configuration.toArray(config.get("ssp", "symbol_stone"));
		scissors = Configuration.toArray(config.get("ssp", "symbol_scissors"));
		paper = Configuration.toArray(config.get("ssp", "symbol_paper"));
	}

	private int getSymbolFor( String value ) {
		for( String v : stone ) {
			if( v.equalsIgnoreCase(value) ) {
				return SYMBOL_STONE;
			}
		}
		for( String v : scissors ) {
			if( v.equalsIgnoreCase(value) ) {
				return SYMBOL_SCISSORS;
			}
		}
		for( String v : paper ) {
			if( v.equalsIgnoreCase(value) ) {
				return SYMBOL_PAPER;
			}
		}
		return SYMBOL_UNKNOWN;
	}

	private Object getNameFor( int symbol ) {
		switch( symbol ) {
			case SYMBOL_STONE:
				return "STONE";
			case SYMBOL_SCISSORS:
				return "SCISSORS";
			case SYMBOL_PAPER:
				return "PAPER";
			default:
				return "UNKNOWN";
		}
	}

	@Override
	public void handleConnect() {
		sendOk(config.fetch("ssp", "greeting"));
	}

	@Override
	public void handleCommand( String command, String value ) throws ProtocolException {
		switch( command ) {
			case "PLAY":
				handlePlay(value);
				break;
			case "SYMBOL":
				handleSymbol(value);
				break;
			case "QUIT":
				handleQuit();
				break;
			default:
				sendErr("command unknown");
				break;
		}
	}

	private void handlePlay( String value ) throws ProtocolException {
		try {
			if( value.isEmpty() ) {
				roundsTotal = config.get("ssp", "default_rounds", int.class);
			} else {
				roundsTotal = Integer.parseInt(value);
				if( roundsTotal % 2 == 0 ) {
					throw new ProtocolException("illegal argument, only 3, 5 or 7 rounds allowed");
				}
			}
			roundsCurrent = 0;
			state = STATE_PLAY;

			sendOk("starting game with %d rounds", roundsTotal);
		} catch( NumberFormatException e ) {
			throw new ProtocolException("illegal argument, number expected");
		}
	}

	private void handleSymbol( String value ) throws ProtocolException {
		if( state != STATE_PLAY ) {
			throw new ProtocolException("command not allowed in this state");
		}

		int symbol = getSymbolFor(value);

		if( symbol == SYMBOL_UNKNOWN ) {
			throw new ProtocolException("unknown symbol, try again");
		}

		boolean win = false;
		int opSymbol = ((rand.nextInt(300) + 1) / 100) + 1;
		if( symbol == opSymbol ) {
			sendOk("result: draw (%s vs %s)", getNameFor(symbol), getNameFor(opSymbol));
			sendScore();
			return;
		} else if( symbol == SYMBOL_STONE && opSymbol == SYMBOL_SCISSORS ) {
			win = true;
		} else if( symbol == SYMBOL_SCISSORS && opSymbol == SYMBOL_PAPER ) {
			win = true;
		} else if( symbol == SYMBOL_PAPER && opSymbol == SYMBOL_STONE ) {
			win = true;
		}

		if( win ) {
			winsTotal += 1;
			sendOk("result: you win (%s vs %s)", getNameFor(symbol), getNameFor(opSymbol));
		} else {
			sendOk("result: opponent wins (%s vs %s)", getNameFor(symbol), getNameFor(opSymbol));
		}

		roundsCurrent += 1;
		if( roundsCurrent >= roundsTotal || winsTotal > roundsTotal / 2 || (roundsCurrent - winsTotal) > roundsTotal / 2 ) {
			if( winsTotal > roundsTotal / 2 ) {
				sendOk("game ended, you win with %d of %d wins", winsTotal, roundsTotal);
			} else {
				sendOk("game ended, you loose with %d of %d wins", winsTotal, roundsTotal);
			}
			state = STATE_INITIALIZING;
		} else {
			sendScore();
		}
	}

	private void sendScore() {
		sendOk("current score: %d of %d rounds played, %d wins", roundsCurrent, roundsTotal, winsTotal);
	}

	private void handleQuit() {
		state = STATE_QUIT;
		sendOk("bye");
		close();
	}

	@Override
	public void handleDisconnect() {
	}

	private void sendOk() {
		send("+");
	}

	private void sendOk( String message ) {
		send("+ %s", message);
	}

	private void sendOk( String message, Object... args ) {
		sendOk(String.format(message, args));
	}

	private void sendErr() {
		send("-");
	}

	private void sendErr( String message ) {
		send("- " + message);
	}

	private void sendErr( String message, Object... args ) {
		sendErr(String.format(message, args));
	}

	@Override
	public void sendError( ProtocolException e ) {
		sendErr(e.getMessage());
	}

}
