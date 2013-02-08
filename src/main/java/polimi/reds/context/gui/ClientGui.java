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

import java.awt.Event;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import polimi.reds.Message;
import polimi.reds.TextFilter;
import polimi.reds.TextMessage;
import polimi.reds.context.Condition;
import polimi.reds.context.Context;
import polimi.reds.context.ContextFilter;
import polimi.reds.context.Property;
import polimi.reds.test.RepliableTextMessage;

public class ClientGui extends JFrame {
	
	private Logger logger;
	
	private String[] operators = { "Condition.EQUALS", "Condition.GREATER",
	// "Condition.INNER",
									"Condition.LOWER", "Condition.NOT_EQUALS",
									// "Condition.NOT_INNER",
									// "Condition.CONTAINS",
									// "Condition.ENDS_WITH",
									// "Condition.STARTS_WITH",
									"Condition.ANY" };

	private HashMap stringToOperator = null;

	private CAClient client = null;

	private JPanel jContentPane = null;

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

	private JTextField txtMessage = null;

	private JLabel lblMessaggio = null;

	private JButton btnSend = null;

	private JComboBox cmbOperatorRAM = null;

	private JComboBox cmbOperatorHD = null;

	private JLabel lblRAM = null;

	private JLabel lblHD = null;

	private JTextField txtHD = null;

	private JTextField txtRAM = null;

	private JLabel lblMioContesto = null;

	private JLabel lblMyRAM = null;

	private JLabel lblMyHD = null;

	private JTextField txtMyRAM = null;

	private JTextField txtMyHD = null;

	private JButton btnSetContext = null;

	private JButton btnConnect = null;

	private JTextField txtPort = null;

	private TextArea txtLog = null;

	private JButton btnSubscribe = null;

	private JButton btnSubscribeContext = null;

	private JButton btnUnsubscribe = null;

	private JTextField txtHost = null;

	private JCheckBox chkReply = null;

	/**
	 * This is the default constructor
	 */
	public ClientGui( int port ) {
		super();
		stringToOperator = new HashMap();
		stringToOperator.put( "Condition.EQUALS", new Integer( Condition.EQUALS ) );
		stringToOperator.put( "Condition.GREATER", new Integer( Condition.GREATER ) );
		stringToOperator.put( "Condition.INNER", new Integer( Condition.INNER ) );
		stringToOperator.put( "Condition.LOWER", new Integer( Condition.LOWER ) );
		stringToOperator.put( "Condition.NOT_EQUALS", new Integer( Condition.NOT_EQUALS ) );
		stringToOperator.put( "Condition.NOT_INNER", new Integer( Condition.NOT_INNER ) );
		stringToOperator.put( "Condition.CONTAINS", new Integer( Condition.CONTAINS ) );
		stringToOperator.put( "Condition.ENDS_WITH", new Integer( Condition.ENDS_WITH ) );
		stringToOperator.put( "Condition.STARTS_WITH", new Integer( Condition.STARTS_WITH ) );
		initialize();
		txtPort.setText( String.valueOf( port ) );
		
		logger = Logger.getLogger( "polimi.reds" );
		logger.addHandler( new TextAreaHandler(txtLog) );
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		this.setResizable( false );
		this.setLocation( new java.awt.Point( 100, 100 ) );
		this.setJMenuBar( getJJMenuBar() );
		this.setSize( 840, 400 );
		this.setContentPane( getJContentPane() );
		this.setTitle( "Client" );
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if ( jContentPane == null ) {
			lblMyHD = new JLabel();
			lblMyHD.setText( "My HD" );
			lblMyHD.setSize( new java.awt.Dimension( 100, 20 ) );
			lblMyHD.setLocation( new java.awt.Point( 10, 290 ) );
			lblMyRAM = new JLabel();
			lblMyRAM.setText( "My RAM" );
			lblMyRAM.setSize( new java.awt.Dimension( 100, 20 ) );
			lblMyRAM.setLocation( new java.awt.Point( 10, 260 ) );
			lblMioContesto = new JLabel();
			lblMioContesto.setHorizontalAlignment( javax.swing.SwingConstants.CENTER );
			lblMioContesto.setForeground( new java.awt.Color( 55, 164, 116 ) );
			lblMioContesto.setBackground( new java.awt.Color( 135, 48, 40 ) );
			lblMioContesto.setLocation( new java.awt.Point( 10, 230 ) );
			lblMioContesto.setSize( new java.awt.Dimension( 210, 20 ) );
			lblMioContesto.setText( "Mio contesto" );
			lblHD = new JLabel();
			lblHD.setText( "HD" );
			lblHD.setSize( new java.awt.Dimension( 100, 20 ) );
			lblHD.setLocation( new java.awt.Point( 10, 70 ) );
			lblRAM = new JLabel();
			lblRAM.setText( "RAM" );
			lblRAM.setLocation( new java.awt.Point( 10, 40 ) );
			lblRAM.setSize( new java.awt.Dimension( 100, 20 ) );
			lblMessaggio = new JLabel();
			lblMessaggio.setText( "Messaggio" );
			lblMessaggio.setSize( new java.awt.Dimension( 100, 20 ) );
			lblMessaggio.setPreferredSize( new java.awt.Dimension( 60, 16 ) );
			lblMessaggio.setLocation( new java.awt.Point( 10, 10 ) );
			jContentPane = new JPanel();
			jContentPane.setLayout( null );
			jContentPane.add( getTxtMessage(), null );
			jContentPane.add( lblMessaggio, null );
			jContentPane.add( getBtnSend(), null );
			jContentPane.add( getCmbOperatorRAM(), null );
			jContentPane.add( getCmbOperatorHD(), null );
			jContentPane.add( lblRAM, null );
			jContentPane.add( lblHD, null );
			jContentPane.add( getTxtHD(), null );
			jContentPane.add( getTxtRAM(), null );
			jContentPane.add( lblMioContesto, null );
			jContentPane.add( lblMyRAM, null );
			jContentPane.add( lblMyHD, null );
			jContentPane.add( getTxtMiaRAM(), null );
			jContentPane.add( getTxtMioHD(), null );
			jContentPane.add( getBtnSetContext(), null );
			jContentPane.add( getBtnConnect(), null );
			jContentPane.add( getTxtPort(), null );
			jContentPane.add( getTxtLog(), null );
			jContentPane.add( getBtnSubscribe(), null );
			jContentPane.add( getBtnSubscribeContext(), null );
			jContentPane.add( getBtnUnsubscribe(), null );
			jContentPane.add( getTxtHost(), null );
			jContentPane.add( getChkReply(), null );
		}
		return jContentPane;
	}

	/**
	 * This method initializes jJMenuBar
	 * 
	 * @return javax.swing.JMenuBar
	 */
	private JMenuBar getJJMenuBar() {
		if ( jJMenuBar == null ) {
			jJMenuBar = new JMenuBar();
			jJMenuBar.add( getFileMenu() );
			jJMenuBar.add( getEditMenu() );
			jJMenuBar.add( getHelpMenu() );
		}
		return jJMenuBar;
	}

	/**
	 * This method initializes jMenu
	 * 
	 * @return javax.swing.JMenu
	 */
	private JMenu getFileMenu() {
		if ( fileMenu == null ) {
			fileMenu = new JMenu();
			fileMenu.setText( "File" );
			fileMenu.add( getSaveMenuItem() );
			fileMenu.add( getExitMenuItem() );
		}
		return fileMenu;
	}

	/**
	 * This method initializes jMenu
	 * 
	 * @return javax.swing.JMenu
	 */
	private JMenu getEditMenu() {
		if ( editMenu == null ) {
			editMenu = new JMenu();
			editMenu.setText( "Edit" );
			editMenu.add( getCutMenuItem() );
			editMenu.add( getCopyMenuItem() );
			editMenu.add( getPasteMenuItem() );
		}
		return editMenu;
	}

	/**
	 * This method initializes jMenu
	 * 
	 * @return javax.swing.JMenu
	 */
	private JMenu getHelpMenu() {
		if ( helpMenu == null ) {
			helpMenu = new JMenu();
			helpMenu.setText( "Help" );
			helpMenu.add( getAboutMenuItem() );
		}
		return helpMenu;
	}

	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getExitMenuItem() {
		if ( exitMenuItem == null ) {
			exitMenuItem = new JMenuItem();
			exitMenuItem.setText( "Exit" );
			exitMenuItem.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent e ) {
					System.exit( 0 );
				}
			} );
		}
		return exitMenuItem;
	}

	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getAboutMenuItem() {
		if ( aboutMenuItem == null ) {
			aboutMenuItem = new JMenuItem();
			aboutMenuItem.setText( "About" );
			aboutMenuItem.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent e ) {
					new JDialog( ClientGui.this, "About", true ).show();
				}
			} );
		}
		return aboutMenuItem;
	}

	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getCutMenuItem() {
		if ( cutMenuItem == null ) {
			cutMenuItem = new JMenuItem();
			cutMenuItem.setText( "Cut" );
			cutMenuItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_X, Event.CTRL_MASK, true ) );
		}
		return cutMenuItem;
	}

	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getCopyMenuItem() {
		if ( copyMenuItem == null ) {
			copyMenuItem = new JMenuItem();
			copyMenuItem.setText( "Copy" );
			copyMenuItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_C, Event.CTRL_MASK, true ) );
		}
		return copyMenuItem;
	}

	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getPasteMenuItem() {
		if ( pasteMenuItem == null ) {
			pasteMenuItem = new JMenuItem();
			pasteMenuItem.setText( "Paste" );
			pasteMenuItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_V, Event.CTRL_MASK, true ) );
		}
		return pasteMenuItem;
	}

	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getSaveMenuItem() {
		if ( saveMenuItem == null ) {
			saveMenuItem = new JMenuItem();
			saveMenuItem.setText( "Save" );
			saveMenuItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_S, Event.CTRL_MASK, true ) );
		}
		return saveMenuItem;
	}

	/**
	 * This method initializes txtMessage
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getTxtMessage() {
		if ( txtMessage == null ) {
			txtMessage = new JTextField();
			txtMessage.setText( "Server" );
			txtMessage.setSize( new java.awt.Dimension( 260, 20 ) );
			txtMessage.setLocation( new java.awt.Point( 120, 10 ) );
		}
		return txtMessage;
	}

	/**
	 * This method initializes btnSend
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getBtnSend() {
		if ( btnSend == null ) {
			btnSend = new JButton();
			btnSend.setText( "Send" );
			btnSend.setSize( new java.awt.Dimension( 150, 20 ) );
			btnSend.setLocation( new java.awt.Point( 230, 100 ) );
			btnSend.addActionListener( new java.awt.event.ActionListener() {
				public void actionPerformed( java.awt.event.ActionEvent e ) {
					sendMessage();
				}

				public void sendMessage() {
					ContextFilter f = createFilter();

					Message message = null;
					;

					if ( chkReply.isSelected() ) {
						message = new RepliableTextMessage( txtMessage.getText() );
						scrivi( "messaggio repliable" );
					}
					else {
						message = new TextMessage( txtMessage.getText() );
						scrivi( "messaggio non repliable" );
					}
					client.publish( message, f );
				}
			} );
		}
		return btnSend;
	}

	/**
	 * This method initializes cmbOperatorGoal
	 * 
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getCmbOperatorRAM() {
		if ( cmbOperatorRAM == null ) {
			cmbOperatorRAM = new JComboBox( operators );
			cmbOperatorRAM.setLocation( new java.awt.Point( 230, 40 ) );
			cmbOperatorRAM.setSize( new java.awt.Dimension( 150, 20 ) );
		}
		return cmbOperatorRAM;
	}

	/**
	 * This method initializes cmbStagioni
	 * 
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getCmbOperatorHD() {
		if ( cmbOperatorHD == null ) {
			cmbOperatorHD = new JComboBox( operators );
			cmbOperatorHD.setLocation( new java.awt.Point( 230, 70 ) );
			cmbOperatorHD.setSize( new java.awt.Dimension( 150, 20 ) );
		}
		return cmbOperatorHD;
	}

	/**
	 * This method initializes txtStagioni
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getTxtHD() {
		if ( txtHD == null ) {
			txtHD = new JTextField();
			txtHD.setText( "30" );
			txtHD.setSize( new java.awt.Dimension( 100, 20 ) );
			txtHD.setLocation( new java.awt.Point( 120, 70 ) );
		}
		return txtHD;
	}

	/**
	 * This method initializes txtGoal
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getTxtRAM() {
		if ( txtRAM == null ) {
			txtRAM = new JTextField();
			txtRAM.setText( "256" );
			txtRAM.setSize( new java.awt.Dimension( 100, 20 ) );
			txtRAM.setLocation( new java.awt.Point( 120, 40 ) );
		}
		return txtRAM;
	}

	/**
	 * This method initializes txtMieiGoal
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getTxtMiaRAM() {
		if ( txtMyRAM == null ) {
			txtMyRAM = new JTextField();
			txtMyRAM.setText( "256" );
			txtMyRAM.setSize( new java.awt.Dimension( 100, 20 ) );
			txtMyRAM.setLocation( new java.awt.Point( 120, 260 ) );
		}
		return txtMyRAM;
	}

	/**
	 * This method initializes txtMieStagioni
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getTxtMioHD() {
		if ( txtMyHD == null ) {
			txtMyHD = new JTextField();
			txtMyHD.setText( "30" );
			txtMyHD.setSize( new java.awt.Dimension( 100, 20 ) );
			txtMyHD.setLocation( new java.awt.Point( 120, 290 ) );
		}
		return txtMyHD;
	}

	/**
	 * This method initializes btnSetContext
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getBtnSetContext() {
		if ( btnSetContext == null ) {
			btnSetContext = new JButton();
			btnSetContext.setText( "Set context" );
			btnSetContext.setSize( new java.awt.Dimension( 210, 20 ) );
			btnSetContext.setLocation( new java.awt.Point( 10, 320 ) );
			btnSetContext.addActionListener( new java.awt.event.ActionListener() {
				public void actionPerformed( java.awt.event.ActionEvent e ) {
					setContext();
				}
			} );
		}
		return btnSetContext;
	}

	/**
	 * This method initializes btnConnect
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getBtnConnect() {
		if ( btnConnect == null ) {
			btnConnect = new JButton();
			btnConnect.setPreferredSize( new java.awt.Dimension( 81, 20 ) );
			btnConnect.setLocation( new java.awt.Point( 650, 320 ) );
			btnConnect.setSize( new java.awt.Dimension( 170, 20 ) );
			btnConnect.setText( "Connect" );
			btnConnect.addActionListener( new java.awt.event.ActionListener() {
				public void actionPerformed( java.awt.event.ActionEvent e ) {
					connect();
				}
			} );
		}
		return btnConnect;
	}

	/**
	 * This method initializes txtPort
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getTxtPort() {
		if ( txtPort == null ) {
			txtPort = new JTextField();
			txtPort.setText( "6000" );
			txtPort.setSize( new java.awt.Dimension( 120, 20 ) );
			txtPort.setLocation( new java.awt.Point( 520, 320 ) );
		}
		return txtPort;
	}

	/**
	 * This method initializes txtLog
	 * 
	 * @return java.awt.TextArea
	 */
	private TextArea getTxtLog() {
		if ( txtLog == null ) {
			txtLog = new TextArea();
			txtLog.setLocation( new java.awt.Point( 390, 10 ) );
			txtLog.setSize( new java.awt.Dimension( 431, 301 ) );
		}
		return txtLog;
	}

	/**
	 * This method initializes btnSubscribe
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getBtnSubscribe() {
		if ( btnSubscribe == null ) {
			btnSubscribe = new JButton();
			btnSubscribe.setText( "Subscribe ANY" );
			btnSubscribe.setSize( new java.awt.Dimension( 181, 21 ) );
			btnSubscribe.setLocation( new java.awt.Point( 10, 100 ) );
			btnSubscribe.addActionListener( new java.awt.event.ActionListener() {
				public void actionPerformed( java.awt.event.ActionEvent e ) {
					subscribe();
				}
			} );
		}
		return btnSubscribe;
	}

	/**
	 * This method initializes btnSubscribeContext
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getBtnSubscribeContext() {
		if ( btnSubscribeContext == null ) {
			btnSubscribeContext = new JButton();
			btnSubscribeContext.setText( "Context-aware Subscribe" );
			btnSubscribeContext.setSize( new java.awt.Dimension( 181, 21 ) );
			btnSubscribeContext.setLocation( new java.awt.Point( 10, 130 ) );
			btnSubscribeContext.addActionListener( new java.awt.event.ActionListener() {
				public void actionPerformed( java.awt.event.ActionEvent e ) {
					subscribe( createFilter() );
				}
			} );
		}
		return btnSubscribeContext;
	}

	/**
	 * This method initializes btnUnsubscribe
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getBtnUnsubscribe() {
		if ( btnUnsubscribe == null ) {
			btnUnsubscribe = new JButton();
			btnUnsubscribe.setText( "Unsubscribe" );
			btnUnsubscribe.setSize( new java.awt.Dimension( 181, 21 ) );
			btnUnsubscribe.setLocation( new java.awt.Point( 10, 160 ) );
			btnUnsubscribe.addActionListener( new java.awt.event.ActionListener() {
				public void actionPerformed( java.awt.event.ActionEvent e ) {
					client.unsubscribeAll();
				}
			} );
		}
		return btnUnsubscribe;
	}

	/**
	 * This method initializes txtHost
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getTxtHost() {
		if ( txtHost == null ) {
			txtHost = new JTextField();
			txtHost.setText( "localhost" );
			txtHost.setSize( new java.awt.Dimension( 120, 20 ) );
			txtHost.setLocation( new java.awt.Point( 390, 320 ) );
		}
		return txtHost;
	}

	/**
	 * This method initializes chkReply
	 * 
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getChkReply() {
		if ( chkReply == null ) {
			chkReply = new JCheckBox();
			chkReply.setText( "Replyable?" );
			chkReply.setSize( new java.awt.Dimension( 150, 20 ) );
			chkReply.setHorizontalAlignment( javax.swing.SwingConstants.RIGHT );
			chkReply.setHorizontalTextPosition( javax.swing.SwingConstants.LEADING );
			chkReply.setLocation( new java.awt.Point( 230, 130 ) );
		}
		return chkReply;
	}

	/**
	 * Launches this application
	 */
	public static void main( String[] args ) {
		ClientGui application = new ClientGui( 6000 );
		application.show();
	}

	public void scrivi( String s ) {
		txtLog.setText( txtLog.getText() + s + "\n" );
		txtLog.setCaretPosition( txtLog.getText().length() );
	}

	public void connect() {
		int port = Integer.parseInt( txtPort.getText() );
		client = new CAClient( txtHost.getText(), port, Context.nullContext, this );
		scrivi( "Client creato correttamente" );
		client.start();
	}

	public void subscribe() {
		TextFilter tf = new TextFilter( txtMessage.getText(), TextFilter.CONTAINS );
		ContextFilter cf = ContextFilter.ANY;
		client.subscribe( tf, cf );
		scrivi( "Mi sono anche registrato per i messaggi di testo che contengono " + txtMessage.getText()
				+ " che arrivano da qualsiasi parte" );
	}

	public void subscribe( ContextFilter cf ) {
		TextFilter tf = new TextFilter( txtMessage.getText(), TextFilter.CONTAINS );
		client.subscribe( tf, cf );
		scrivi( "Mi sono anche registrato per i messaggi di testo che contengono " + txtMessage.getText()
				+ " provenienti dal contesto " + cf );
	}

	public void setContext() {
		Property ram = new Property( lblRAM.getText(), Property.INTEGER, Integer.parseInt( txtMyRAM.getText() ) );
		Property hd = new Property( lblHD.getText(), Property.INTEGER, Integer.parseInt( txtMyHD.getText() ) );

		Context myContext = new Context();
		myContext.addProperty( ram );
		myContext.addProperty( hd );

		client.setContext( myContext );
		scrivi( "Ho settato il contesto: \n" + myContext );
	}

	public void setContext( int ram, int hd ) {
		txtMyRAM.setText( String.valueOf( ram ) );
		txtMyHD.setText( String.valueOf( hd ) );
		setContext();
	}

	private Condition createRAMCondition() {
		Condition ram;
		if ( cmbOperatorRAM.getSelectedItem().equals( "Condition.ANY" ) ) {
			ram = Condition.CreateANYCondition( lblRAM.getText(), Property.INTEGER );
		}
		else {
			int value = ( (Integer) stringToOperator.get( cmbOperatorRAM.getSelectedItem() ) ).intValue();
			ram = new Condition( lblRAM.getText(), Property.INTEGER, value, new Integer( txtRAM.getText() ) );
		}
		return ram;
	}

	private Condition createHDCondition() {
		Condition hd;
		if ( cmbOperatorHD.getSelectedItem().equals( "Condition.ANY" ) ) {
			hd = Condition.CreateANYCondition( lblHD.getText(), Property.INTEGER );
		}
		else {
			int value = ( (Integer) stringToOperator.get( cmbOperatorHD.getSelectedItem() ) ).intValue();
			hd = new Condition( lblHD.getText(), Property.INTEGER, value, new Integer( txtHD.getText() ) );
		}
		return hd;
	}

	private ContextFilter createFilter() {
		Condition ram = createRAMCondition();

		Condition hd = createHDCondition();

		ContextFilter f = new ContextFilter();
		f.addCondition( ram );
		f.addCondition( hd );
		return f;
	}

} // @jve:decl-index=0:visual-constraint="10,10"
