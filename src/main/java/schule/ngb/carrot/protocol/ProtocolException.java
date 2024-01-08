package schule.ngb.carrot.protocol;

public class ProtocolException extends Exception {

	private final int code;

	public ProtocolException( String message ) {
		super(message);
		this.code = 0;
	}

	public ProtocolException( int code, String message ) {
		super(message);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public String toString() {
		if( this.code != 0 ) {
			return this.code + " " + this.getMessage();
		} else {
			return this.getMessage();
		}
	}

}
