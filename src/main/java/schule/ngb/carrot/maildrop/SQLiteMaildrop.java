package schule.ngb.carrot.maildrop;

import java.util.List;

public class SQLiteMaildrop implements Maildrop {

	public final class Factory implements MaildropFactory {

		@Override
		public Maildrop create( String username ) throws MaildropException {
			return new SQLiteMaildrop();
		}

	}

	@Override
	public Mail getMail( int number ) throws MaildropException {
		return null;
	}

	@Override
	public List<Mail> listMails() throws MaildropException {
		return null;
	}

	@Override
	public List<Mail> listAllMails() throws MaildropException {
		return null;
	}

	@Override
	public long countAll() throws MaildropException {
		return 0;
	}

	@Override
	public long count() throws MaildropException {
		return 0;
	}

	@Override
	public long size() throws MaildropException {
		return 0;
	}

	@Override
	public long size( int number ) throws MaildropException {
		return 0;
	}

	@Override
	public String getText( int number ) throws MaildropException {
		return null;
	}

	@Override
	public List<String> getLines( int number ) throws MaildropException {
		return null;
	}

	@Override
	public void deleteFile( int number ) throws MaildropException {

	}

	@Override
	public void resetDeleted() {

	}

	@Override
	public void executeDelete() throws MaildropException {

	}

	@Override
	public void restoreDeleted() throws MaildropException {

	}

	@Override
	public void lock() throws MaildropException {

	}

	@Override
	public void unlock() throws MaildropException {

	}

	@Override
	public boolean isLocked() {
		return false;
	}

	@Override
	public void createMail( String content ) throws MaildropException {

	}

}
