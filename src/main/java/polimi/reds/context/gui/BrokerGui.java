/***
 * * REDS - REconfigurable Dispatching System
 * * Copyright (C) 2003 Politecnico di Milano
 * * <mailto: cugola@elet.polimi.it> <mailto: picco@elet.polimi.it>
 * *
 * * This library is free software; you can redistribute it and/or modify it
 * * under the terms of the GNU Lesser General Public License as published by
 * * the Free Software Foundation; either version 2.1 of the License, or (at
 * * your option) any later version.
 * *
 * * This library is distributed in the hope that it will be useful, but
 * * WITHOUT ANY WARRANTY; without even the implied warranty of
 * * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * * General Public License for more details.
 * *
 * * You should have received a copy of the GNU Lesser General Public License
 * * along with this library; if not, write to the Free Software Foundation,
 * * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 ***/

package polimi.reds.context.gui;

import java.awt.CardLayout;
import java.awt.Event;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

public class BrokerGui extends JFrame {

	private Logger logger;

	private CABroker broker = null;

	private BrokerGui thisGui = null;

	private JMenuBar jJMenuBar = null;

	private JMenu fileMenu = null;

	private JMenu editMenu = null;

	private JMenu helpMenu = null;

	private JMenuItem exitMenuItem = null;

	private JMenuItem aboutMenuItem = null;

	private JMenuItem cutMenuItem = null;

	private JMenuItem copyMenuItem = null;

	private JMenuItem pasteMenuItem = null;

	private JMenuItem saveMenuItem = null;

	private JSplitPane jSplitPane = null;

	private JPanel jPanel = null;

	private JPanel jPanel1 = null;

	private JTextField txtListenPort = null;

	private JLabel lblPort = null;

	private JTextField txtConnectPort = null;

	private JLabel lblConnectPort = null;

	private JButton btnStartListen = null;

	private JButton btnConnectTo = null;

	private TextArea txtLog = null;

	private JTextField txtHost = null;

	/**
	 * This is the default constructor
	 */
	public BrokerGui(int listenPort) {
		super();
		thisGui = this;
		initialize();
		txtListenPort.setText(String.valueOf(listenPort));
		logger = Logger.getLogger("polimi.reds");
		logger.setLevel(Level.FINE);
		logger.addHandler(new TextAreaHandler(txtLog));
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		this.setLocation(new java.awt.Point(200, 150));
		this.setContentPane(getJSplitPane());
		this.setJMenuBar(getJJMenuBar());
		this.setSize(895, 496);
		this.setTitle("Broker");
	}

	/**
	 * This method initializes jJMenuBar
	 * 
	 * @return javax.swing.JMenuBar
	 */
	private JMenuBar getJJMenuBar() {
		if (jJMenuBar == null) {
			jJMenuBar = new JMenuBar();
			jJMenuBar.add(getFileMenu());
			jJMenuBar.add(getEditMenu());
			jJMenuBar.add(getHelpMenu());
		}
		return jJMenuBar;
	}

	/**
	 * This method initializes jMenu
	 * 
	 * @return javax.swing.JMenu
	 */
	private JMenu getFileMenu() {
		if (fileMenu == null) {
			fileMenu = new JMenu();
			fileMenu.setText("File");
			fileMenu.add(getSaveMenuItem());
			fileMenu.add(getExitMenuItem());
		}
		return fileMenu;
	}

	/**
	 * This method initializes jMenu
	 * 
	 * @return javax.swing.JMenu
	 */
	private JMenu getEditMenu() {
		if (editMenu == null) {
			editMenu = new JMenu();
			editMenu.setText("Edit");
			editMenu.add(getCutMenuItem());
			editMenu.add(getCopyMenuItem());
			editMenu.add(getPasteMenuItem());
		}
		return editMenu;
	}

	/**
	 * This method initializes jMenu
	 * 
	 * @return javax.swing.JMenu
	 */
	private JMenu getHelpMenu() {
		if (helpMenu == null) {
			helpMenu = new JMenu();
			helpMenu.setText("Help");
			helpMenu.add(getAboutMenuItem());
		}
		return helpMenu;
	}

	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getExitMenuItem() {
		if (exitMenuItem == null) {
			exitMenuItem = new JMenuItem();
			exitMenuItem.setText("Exit");
			exitMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});
		}
		return exitMenuItem;
	}

	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getAboutMenuItem() {
		if (aboutMenuItem == null) {
			aboutMenuItem = new JMenuItem();
			aboutMenuItem.setText("About");
			aboutMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					new JDialog(BrokerGui.this, "About", true).show();
				}
			});
		}
		return aboutMenuItem;
	}

	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getCutMenuItem() {
		if (cutMenuItem == null) {
			cutMenuItem = new JMenuItem();
			cutMenuItem.setText("Cut");
			cutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Event.CTRL_MASK, true));
		}
		return cutMenuItem;
	}

	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getCopyMenuItem() {
		if (copyMenuItem == null) {
			copyMenuItem = new JMenuItem();
			copyMenuItem.setText("Copy");
			copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK, true));
		}
		return copyMenuItem;
	}

	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getPasteMenuItem() {
		if (pasteMenuItem == null) {
			pasteMenuItem = new JMenuItem();
			pasteMenuItem.setText("Paste");
			pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Event.CTRL_MASK, true));
		}
		return pasteMenuItem;
	}

	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getSaveMenuItem() {
		if (saveMenuItem == null) {
			saveMenuItem = new JMenuItem();
			saveMenuItem.setText("Save");
			saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK, true));
		}
		return saveMenuItem;
	}

	/**
	 * This method initializes jSplitPane
	 * 
	 * @return javax.swing.JSplitPane
	 */
	private JSplitPane getJSplitPane() {
		if (jSplitPane == null) {
			jSplitPane = new JSplitPane();
			jSplitPane.setDividerLocation(140);
			jSplitPane.setLeftComponent(getJPanel());
			jSplitPane.setRightComponent(getJPanel1());
		}
		return jSplitPane;
	}

	/**
	 * This method initializes jPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanel() {
		if (jPanel == null) {
			lblConnectPort = new JLabel();
			lblConnectPort.setText("Connect to");
			lblConnectPort.setSize(new java.awt.Dimension(120, 20));
			lblConnectPort.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
			lblConnectPort.setLocation(new java.awt.Point(10, 150));
			lblPort = new JLabel();
			lblPort.setText("Listen port");
			lblPort.setSize(new java.awt.Dimension(120, 20));
			lblPort.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
			lblPort.setLocation(new java.awt.Point(10, 10));
			jPanel = new JPanel();
			jPanel.setLayout(null);
			jPanel.add(getTxtListenPort(), null);
			jPanel.add(lblPort, null);
			jPanel.add(getTxtConnectPort(), null);
			jPanel.add(lblConnectPort, null);
			jPanel.add(getBtnStartListen(), null);
			jPanel.add(getBtnConnectTo(), null);
			jPanel.add(getTxtHost(), null);
		}
		return jPanel;
	}

	/**
	 * This method initializes jPanel1
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jPanel1 = new JPanel();
			jPanel1.setLayout(new CardLayout());
			jPanel1.add(getTxtLog(), getTxtLog().getName());
		}
		return jPanel1;
	}

	/**
	 * This method initializes txtPort
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getTxtListenPort() {
		if (txtListenPort == null) {
			txtListenPort = new JTextField();
			txtListenPort.setText("6000");
			txtListenPort.setSize(new java.awt.Dimension(120, 20));
			txtListenPort.setLocation(new java.awt.Point(10, 40));
		}
		return txtListenPort;
	}

	/**
	 * This method initializes txtConnectPort
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getTxtConnectPort() {
		if (txtConnectPort == null) {
			txtConnectPort = new JTextField();
			txtConnectPort.setPreferredSize(new java.awt.Dimension(120, 20));
			txtConnectPort.setLocation(new java.awt.Point(10, 210));
			txtConnectPort.setSize(new java.awt.Dimension(120, 20));
			txtConnectPort.setText("6001");
		}
		return txtConnectPort;
	}

	/**
	 * This method initializes btnStartListen
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getBtnStartListen() {
		if (btnStartListen == null) {
			btnStartListen = new JButton();
			btnStartListen.setText("Start listen");
			btnStartListen.setSize(new java.awt.Dimension(120, 20));
			btnStartListen.setLocation(new java.awt.Point(10, 70));
			btnStartListen.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					startListen();
				}
			});
		}
		return btnStartListen;
	}

	/**
	 * This method initializes btnConnectTo
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getBtnConnectTo() {
		if (btnConnectTo == null) {
			btnConnectTo = new JButton();
			btnConnectTo.setText("Connect");
			btnConnectTo.setSize(new java.awt.Dimension(120, 20));
			btnConnectTo.setLocation(new java.awt.Point(10, 240));
			btnConnectTo.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					connect();
				}
			});
		}
		return btnConnectTo;
	}

	/**
	 * This method initializes txtLog
	 * 
	 * @return java.awt.TextArea
	 */
	private TextArea getTxtLog() {
		if (txtLog == null) {
			txtLog = new TextArea();
			txtLog.setName("txtLog");
		}
		return txtLog;
	}

	/**
	 * This method initializes txtHost
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getTxtHost() {
		if (txtHost == null) {
			txtHost = new JTextField();
			txtHost.setPreferredSize(new java.awt.Dimension(120, 20));
			txtHost.setLocation(new java.awt.Point(10, 180));
			txtHost.setSize(new java.awt.Dimension(120, 20));
			txtHost.setText("localhost");
		}
		return txtHost;
	}

	/**
	 * Launches this application
	 */
	public static void main(String[] args) {
		BrokerGui application = new BrokerGui(6000);
		application.show();
	}

	public void scrivi(String s) {
		txtLog.setText(txtLog.getText() + s + "\n");
		txtLog.setCaretPosition(txtLog.getText().length());
	}

	public void startListen() {
		int listeningPort = Integer.parseInt(txtListenPort.getText());
		broker = new CABroker(listeningPort);
		scrivi("Broker creato sulla porta " + listeningPort + " in ascolto");
		thisGui.setTitle(thisGui.getTitle() + " ... " + listeningPort);
	}

	public void connect() {
		this.connect(txtHost.getText(), Integer.parseInt(txtConnectPort.getText()));
	}

	public void connect(String host, int port) {
		try {
			broker.connect(host, port);
			scrivi("Mi sono connesso alla porta" + port);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public void connect(int port) {
		connect("localhost", port);
	}

} // @jve:decl-index=0:visual-constraint="0,0"
