package schule.ngb.carrot.maildrop;

public interface MaildropFactory {

	Maildrop create(String username) throws MaildropException;

}
