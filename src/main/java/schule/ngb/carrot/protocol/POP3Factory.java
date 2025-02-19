package schule.ngb.carrot.protocol;

import org.ini4j.Ini;
import schule.ngb.carrot.maildrop.FilesystemMaildrop;
import schule.ngb.carrot.maildrop.Maildrop;
import schule.ngb.carrot.maildrop.MaildropException;
import schule.ngb.carrot.util.Configuration;
import schule.ngb.carrot.util.Log;

import java.net.Socket;

public class POP3Factory extends GenericProtocolHandlerFactory {

	private static final Log LOG = Log.getLogger(POP3Factory.class);


	public POP3Factory( Ini config ) {
		super(config, POP3Handler.class);
		restoreMails();
	}

	@Override
	public ProtocolHandler create( Socket clientSocket ) {
		return new POP3Handler(clientSocket, this.config);
	}

	public void restoreMails() {
		boolean restore = config.get("pop3", "restore_trash_on_start", boolean.class);
		for( String user : config.get("users").keySet() ) {
			try {
				Maildrop maildrop = new FilesystemMaildrop(user, config);
				if( maildrop.isLocked() ) {
					maildrop.unlock();
				}

				if( restore ) {
					maildrop.restoreDeleted();
					LOG.debug("Restored mails from trash for user %s", user);
				}
			} catch( MaildropException ex ) {
				LOG.error(ex, "failed to restore mails for user %s", user);
			}
		}
	}

}
