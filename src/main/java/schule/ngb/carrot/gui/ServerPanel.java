package schule.ngb.carrot.gui;

import schule.ngb.carrot.Server;
import schule.ngb.carrot.events.ServerEvent;
import schule.ngb.carrot.events.ServerListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;

/**
 * Eine GUI-Komponente, die Informationen über den Status eines der Protokoll-Server anzeigt.
 * <p>
 * Das Panel zeigt den Status und Kontrollelemente für einen {@link Server} an.
 */
public class ServerPanel extends JPanel implements ActionListener, ServerListener {

	private static final String ICON_STATUS_RUNNING = "🟢";

	private static final String ICON_STATUS_STOPPED = "🔴";

	private static final String ICON_STATUS_PENDING = "🟡";

	private static final Color COLOR_STATUS_RUNNING = new Color(0, 164, 101);

	private static final Color COLOR_STATUS_STOPPED = new Color(255, 60, 19);

	private static final Color COLOR_STATUS_PENDING = new Color(239, 191, 77);


	private static final String TEXT_START = "Starten";

	private static final String TEXT_STOP = "Anhalten";

	private static final String TEXT_DISCONNECT = "Alle trennen";

	private static final String TEXT_CONNECTION = "%s Verbindungen";


	/**
	 * Der zugehörige Server.
	 */
	private final Server server;

	/**
	 * Zähler für die aktuelle Anzahl an Verbindungen.
	 */
	private int connections = 0;

	/**
	 * Erstellt eine neue Komponente für den angegebenen Server.
	 *
	 * @param server Der Server.
	 */
	public ServerPanel( Server server ) {
		this.server = server;
		this.server.addListener(this);

		createComponents();
	}


	private JLabel jlStatusIcon, jlConnectionCount;

	private JSpinner jspPort;

	private JButton jbStartStop, jbDisconnect;

	/**
	 * Erstellt die Swing-Komponenten.
	 */
	private void createComponents() {
		this.setBorder(
			BorderFactory.createTitledBorder(
				BorderFactory.createBevelBorder(BevelBorder.RAISED),
				server.getProtocolName()
			)
		);
		this.setBackground(CarrotGUI.COLOR_CARROT_GREEN_LIGHT);

		jlStatusIcon = new JLabel();
		try( InputStream in = ServerPanel.class.getResourceAsStream("carrot_32.png") ) {
			if( in != null ) {
				jlStatusIcon.setIcon(new ImageIcon(ImageIO.read(in)));
			}
		} catch( IOException e ) {
		}

		jspPort = new JSpinner(new SpinnerNumberModel(server.getPort(), 0, 65535, 1));
		jspPort.setEditor(new JSpinner.NumberEditor(jspPort, "0"));

		jbStartStop = new JButton(TEXT_START);
		jbStartStop.setActionCommand("STARTSTOP");
		jbStartStop.addActionListener(this);

		jbDisconnect = new JButton(TEXT_DISCONNECT);
		jbDisconnect.setActionCommand("DISCONNECT");
		jbDisconnect.addActionListener(this);

		jlConnectionCount = new JLabel();
		updateConnectionCount();

		if( server.isRunning() ) {
			this.setStatusRunning();
		} else {
			this.setStatusStopped();
		}

		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbl.setConstraints(this, gbc);
		this.setLayout(gbl);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets = new Insets(2, 4, 2, 4);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 2;
		this.add(jlStatusIcon, gbc);

		gbc.gridx = 1;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.gridheight = 1;
		this.add(jspPort, gbc);

		gbc.gridy += 1;
		this.add(jlConnectionCount, gbc);

		gbc.gridx = 0;
		gbc.gridy += 1;
		this.add(jbDisconnect, gbc);

		gbc.gridy += 1;
		this.add(jbStartStop, gbc);
	}

	/**
	 * Stellt den Zustand der Komponenten auf "gestartet" ein. Die Kind-Komponenten werden
	 * entsprechend aktiviert / deaktiviert.
	 */
	private void setStatusRunning() {
		jlStatusIcon.setEnabled(true);
		jspPort.setEnabled(false);
		jbStartStop.setText(TEXT_STOP);
		jbDisconnect.setEnabled(true);
		jlConnectionCount.setEnabled(true);
		this.setBackground(CarrotGUI.COLOR_CARROT_GREEN_LIGHT);
	}

	/**
	 * Stellt den Zustand der Komponenten auf "gestoppt" ein. Die Kind-Komponenten werden
	 * entsprechend aktiviert / deaktiviert.
	 */
	private void setStatusStopped() {
		jlStatusIcon.setEnabled(false);
		jspPort.setEnabled(true);
		jbStartStop.setText(TEXT_START);
		jbDisconnect.setEnabled(false);
		jlConnectionCount.setEnabled(false);
		this.setBackground(new Color(240, 240, 240));
	}

	/**
	 * Aktualisiert die Anzeige der aktuell bestehenden Verbindungen.
	 */
	public void updateConnectionCount() {
		jlConnectionCount.setText(String.format(TEXT_CONNECTION, this.connections));
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if( e.getActionCommand().equals("STARTSTOP") ) {
//			jlStatusIcon.setText(ICON_STATUS_PENDING);
			if( server.isRunning() ) {
				SwingUtilities.invokeLater(() -> {
					server.close();
				});
			} else {
				SwingUtilities.invokeLater(() -> {
					server.setPort((int) jspPort.getValue());
					server.start();
				});
			}
		} else if( e.getActionCommand().equals("DISCONNECT") ) {
			server.disconnectAll();
		}
	}

	@Override
	public void serverStarted( ServerEvent e ) {
		this.setStatusRunning();

		this.connections = 0;
		updateConnectionCount();
	}

	@Override
	public void serverStopped( ServerEvent e ) {
		this.setStatusStopped();

		this.connections = 0;
		updateConnectionCount();
	}

	@Override
	public void clientConnected( ServerEvent e ) {
		this.connections++;
		updateConnectionCount();
	}

	@Override
	public void clientDisconnected( ServerEvent e ) {
		if( this.connections > 0 ) {
			this.connections--;
		}
		updateConnectionCount();
	}

	@Override
	public void clientTimeout( ServerEvent e ) {

	}

}
