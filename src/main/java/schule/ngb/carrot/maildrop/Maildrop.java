package schule.ngb.carrot.maildrop;

import java.util.List;

public interface Maildrop {

	Mail getMail( int number ) throws MaildropException;

	List<Mail> listMails() throws MaildropException;

	List<Mail> listAllMails() throws MaildropException;

	long countAll() throws MaildropException;

	long count() throws MaildropException;

	long size() throws MaildropException;

	long size( int number ) throws MaildropException;

	String getText( int number ) throws MaildropException;

	List<String> getLines( int number ) throws MaildropException;

	void deleteFile( int number ) throws MaildropException;

	void resetDeleted();

	void executeDelete() throws MaildropException;

	void restoreDeleted() throws MaildropException;

	void lock() throws MaildropException;

	void unlock() throws MaildropException;

	boolean isLocked();

	void createMail( String content ) throws MaildropException;

}
