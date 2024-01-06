package schule.ngb.carrot.maildrop;

import java.io.IOException;

public class MaildropException extends IOException {

	public MaildropException() {
	}

	public MaildropException( String message ) {
		super(message);
	}

	public MaildropException( String message, Throwable cause ) {
		super(message, cause);
	}

	public MaildropException( Throwable cause ) {
		super(cause);
	}

}
