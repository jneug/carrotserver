package schule.ngb.carrot.gui;

import org.ini4j.Ini;
import schule.ngb.carrot.Server;
import schule.ngb.carrot.CarrotServer;
import schule.ngb.carrot.util.Configuration;
import schule.ngb.carrot.util.Log;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

/**
 * GUI der <strong>CarrotServ</strong> App.
 *
 * Falls der
 */
public class CarrotGUI extends JFrame implements WindowListener, ActionListener, ListSelectionListener {

	private static final Log LOG = Log.getLogger(CarrotGUI.class);

	public static final Color COLOR_CARROT_ORANGE = new Color(229, 110, 36);
	public static final Color COLOR_CARROT_ORANGE_LIGHT = new Color(253, 231, 215);
	public static final Color COLOR_CARROT_GREEN = new Color(112, 214, 91);
	public static final Color COLOR_CARROT_GREEN_LIGHT = new Color(212, 252, 169);

	public static final void setLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch( Exception ex ) {
			LOG.error(ex, "Couldn't set the look and feel: %s", ex.getMessage());
		}
	}


	private final CarrotServer app;

	private final Ini config;

	public CarrotGUI( String title, CarrotServer app, Ini config ) {
		super(title);
		this.app = app;
		this.config = config;

		this.createComponents();
		this.setIcon();

		// Methoden der Klasse JFrame
		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		this.addWindowListener(this);
		this.setResizable(false);
		this.setLocationByPlatform(true);
//		this.setLocationRelativeTo(null);
	}

	private void setIcon() {
		final String name = System.getProperty("os.name");

		// Das Icon des Fensters ändern
		try {
			if( name.contains("Mac") ) {
				InputStream iconStream = CarrotGUI.class.getResourceAsStream("dock-icon.png");
				if( iconStream != null ) {
					Image icon = ImageIO.read(iconStream);
					// Dock Icon in macOS setzen
					Taskbar taskbar = Taskbar.getTaskbar();
					taskbar.setIconImage(icon);
				} else {
					LOG.warn("Could not load dock-icon");
				}
			} else {
				ArrayList<Image> icons = new ArrayList<>(4);
				for( int size : new int[]{32, 64, 128, 512} ) {
					URL icnUrl = CarrotGUI.class.getResource("carrot_" + size + ".png");
					if( icnUrl != null ) {
						icons.add(ImageIO.read(icnUrl));
					}
				}

				if( icons.isEmpty() ) {
					LOG.warn("Could not load dock-icon");
				} else {
					this.setIconImages(icons);
				}
			}
		} catch( IllegalArgumentException | IOException e ) {
			LOG.warn("Could not load image icons: %s", e.getMessage());
		} catch( SecurityException | UnsupportedOperationException macex ) {
			// Dock Icon in macOS konnte nicht gesetzt werden :(
			LOG.warn("Could not set dock icon: %s", macex.getMessage());
		}
	}

	private void quitApp() {
		app.shutdown();
		this.setVisible(false);
		this.dispose();
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if( e.getActionCommand().equals("QUIT") ) {
			quitApp();
		}
	}

	@Override
	public void valueChanged( ListSelectionEvent e ) {

	}

	@Override
	public void windowOpened( WindowEvent e ) {

	}

	@Override
	public void windowClosing( WindowEvent e ) {
		quitApp();
	}

	@Override
	public void windowClosed( WindowEvent e ) {

	}

	@Override
	public void windowIconified( WindowEvent e ) {

	}

	@Override
	public void windowDeiconified( WindowEvent e ) {

	}

	@Override
	public void windowActivated( WindowEvent e ) {

	}

	@Override
	public void windowDeactivated( WindowEvent e ) {

	}

	private JButton jbQuit;

	private void createComponents() {
		int gridCols = 3;
		int gridRows = app.getServices().size() / gridCols;
		if( app.getServices().size() % gridCols > 0 ) {
			gridRows += 1;
		}

		int pad = 8;

		JPanel serverPanel = new JPanel();
		serverPanel.setBackground(COLOR_CARROT_ORANGE_LIGHT);
		serverPanel.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));
		serverPanel.setLayout(new GridLayout(gridRows, gridCols, pad, pad));
		for( Server server : app.getServices() ) {
			serverPanel.add(new ServerPanel(server));
		}

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

		jbQuit = new JButton("Server stoppen");
		jbQuit.setActionCommand("QUIT");
		jbQuit.addActionListener(this);
		buttonPanel.add(jbQuit);

		this.setBackground(COLOR_CARROT_ORANGE);
		this.add(serverPanel, BorderLayout.CENTER);
		this.add(buttonPanel, BorderLayout.SOUTH);
		//this.setContentPane(mainPanel);
		this.pack();
	}

}
