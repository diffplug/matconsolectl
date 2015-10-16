/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;
import matlabcontrol.PermissiveSecurityManager;

/**
 * A GUI example to demonstrate the main functionality of controlling MATLAB with matlabcontrol. The code in this
 * class (and the rest of the package) is not intended to serve as an example for how to use matlabcontrol, instead
 * the application exists to interactively demonstrate the API's capabilities.
 * <br><br>
 * The icon is part of the Um collection created by <a href="mailto:mattahan@gmail.com">mattahan (Paul Davey)</a>. It is
 * licensed under the <a href="http://creativecommons.org/licenses/by-nc-sa/3.0/">CC Attribution-Noncommercial-Share
 * Alike 3.0 License</a>.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
@SuppressWarnings("serial")
public class DemoFrame extends JFrame {
	//Status messages
	private static final String STATUS_DISCONNECTED = "Connection Status: Disconnected",
			STATUS_CONNECTING = "Connection Status: Connecting",
			STATUS_CONNECTED_EXISTING = "Connection Status: Connected (Existing)",
			STATUS_CONNECTED_LAUNCHED = "Connection Status: Connected (Launched)";

	//Return messages
	private static final String RETURNED_DEFAULT = "Returned Object / Java Exception",
			RETURNED_OBJECT = "Returned Object",
			RETURNED_EXCEPTION = "Java Exception";

	//Panel/Pane sizes
	private static final int PANEL_WIDTH = 660;
	private static final Dimension CONNECTION_PANEL_SIZE = new Dimension(PANEL_WIDTH, 70),
			RETURN_PANEL_SIZE = new Dimension(PANEL_WIDTH, 250),
			METHOD_PANEL_SIZE = new Dimension(PANEL_WIDTH, 110 + 28 * ArrayPanel.NUM_ENTRIES),
			DESCRIPTION_PANE_SIZE = new Dimension(PANEL_WIDTH, 200),
			COMMAND_PANEL_SIZE = new Dimension(PANEL_WIDTH, METHOD_PANEL_SIZE.height + DESCRIPTION_PANE_SIZE.height),
			MAIN_PANEL_SIZE = new Dimension(PANEL_WIDTH, CONNECTION_PANEL_SIZE.height + COMMAND_PANEL_SIZE.height + RETURN_PANEL_SIZE.height);
	//Factory to create proxy
	private final MatlabProxyFactory _factory;

	//Proxy to communicate with MATLAB
	private final AtomicReference<MatlabProxy> _proxyHolder = new AtomicReference<MatlabProxy>();

	//UI components
	private JButton _invokeButton;
	private JScrollPane _returnPane;
	private JTextArea _returnArea;

	public static BufferedImage getIcon() {
		try {
			return ImageIO.read(DemoFrame.class.getResource("/matlabcontrol/demo/logo_128.png"));
		} catch (Exception e) {
			e.printStackTrace();
			return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		}
	}

	/**
	 * Create the main GUI.
	 */
	public DemoFrame(String title, String matlabLocation) {
		super(title);

		System.setSecurityManager(new PermissiveSecurityManager());
		setIconImage(getIcon());

		//Panel that contains the over panels
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.setBackground(Color.WHITE);
		mainPanel.setPreferredSize(MAIN_PANEL_SIZE);
		mainPanel.setSize(MAIN_PANEL_SIZE);
		this.add(mainPanel);

		//Connection panel, button to connect, progress bar
		final JPanel connectionPanel = new JPanel();
		connectionPanel.setBackground(mainPanel.getBackground());
		connectionPanel.setBorder(BorderFactory.createTitledBorder(STATUS_DISCONNECTED));
		connectionPanel.setPreferredSize(CONNECTION_PANEL_SIZE);
		connectionPanel.setSize(CONNECTION_PANEL_SIZE);
		final JButton connectionButton = new JButton("Connect");
		connectionPanel.add(connectionButton);
		final JProgressBar connectionBar = new JProgressBar();
		connectionPanel.add(connectionBar);

		//To display what has been returned from MATLAB
		_returnArea = new JTextArea();
		_returnArea.setEditable(false);

		//Put the returnArea in pane, add it
		_returnPane = new JScrollPane(_returnArea);
		_returnPane.setBackground(Color.WHITE);
		_returnPane.setPreferredSize(RETURN_PANEL_SIZE);
		_returnPane.setBorder(BorderFactory.createTitledBorder(RETURNED_DEFAULT));

		//Command Panel
		JPanel commandPanel = this.createCommandPanel();

		//Structure the panels so that on resize the layout updates appropriately
		JPanel combinedPanel = new JPanel(new BorderLayout());
		combinedPanel.add(connectionPanel, BorderLayout.NORTH);
		combinedPanel.add(commandPanel, BorderLayout.SOUTH);
		mainPanel.add(combinedPanel, BorderLayout.NORTH);
		mainPanel.add(_returnPane, BorderLayout.CENTER);

		this.pack();

		//Create proxy factory
		MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
				.setUsePreviouslyControlledSession(true)
				.setMatlabLocation(matlabLocation)
				.build();
		_factory = new MatlabProxyFactory(options);

		//Connect to MATLAB when the Connect button is pressed
		connectionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					//Request a proxy
					_factory.requestProxy(new MatlabProxyFactory.RequestCallback() {
						@Override
						public void proxyCreated(final MatlabProxy proxy) {
							_proxyHolder.set(proxy);

							proxy.addDisconnectionListener(new MatlabProxy.DisconnectionListener() {
								@Override
								public void proxyDisconnected(MatlabProxy proxy) {
									_proxyHolder.set(null);

									//Visual update
									EventQueue.invokeLater(new Runnable() {
										@Override
										public void run() {
											connectionPanel.setBorder(BorderFactory.createTitledBorder(STATUS_DISCONNECTED));
											_returnPane.setBorder(BorderFactory.createTitledBorder(RETURNED_DEFAULT));
											_returnArea.setText("");
											connectionBar.setValue(0);
											connectionButton.setEnabled(true);
											_invokeButton.setEnabled(false);
										}
									});
								}
							});

							//Visual update
							EventQueue.invokeLater(new Runnable() {
								@Override
								public void run() {
									String status;
									if (proxy.isExistingSession()) {
										status = STATUS_CONNECTED_EXISTING;
									} else {
										status = STATUS_CONNECTED_LAUNCHED;
									}
									connectionPanel.setBorder(BorderFactory.createTitledBorder(status));

									connectionBar.setValue(100);
									connectionBar.setIndeterminate(false);
									_invokeButton.setEnabled(true);
								}
							});
						}
					});

					//Update GUI
					connectionPanel.setBorder(BorderFactory.createTitledBorder(STATUS_CONNECTING));
					connectionBar.setIndeterminate(true);
					connectionButton.setEnabled(false);
				} catch (MatlabConnectionException exc) {
					_returnPane.setBorder(BorderFactory.createTitledBorder(RETURNED_EXCEPTION));
					_returnArea.setText(ReturnFormatter.formatException(exc));
					_returnArea.setCaretPosition(0);
				}
			}
		});

		//Window events
		this.addWindowListener(new WindowAdapter() {
			/**
			 * On window appearance set the minimum size to the same width and 80% of the height.
			 */
			@Override
			public void windowOpened(WindowEvent e) {
				Dimension size = DemoFrame.this.getSize();
				size.height *= .8;
				DemoFrame.this.setMinimumSize(size);
			}
		});
	}

	/**
	 * Create the command panel.
	 * 
	 * @param returnArea
	 * @return
	 */
	private JPanel createCommandPanel() {
		//Panel that contains the methods, input, and description
		JPanel commandPanel = new JPanel(new BorderLayout());
		commandPanel.setBackground(Color.WHITE);
		commandPanel.setPreferredSize(COMMAND_PANEL_SIZE);
		commandPanel.setSize(COMMAND_PANEL_SIZE);

		//Method
		JPanel methodPanel = new JPanel(new BorderLayout());
		methodPanel.setBorder(BorderFactory.createTitledBorder("Method"));
		methodPanel.setBackground(Color.WHITE);
		methodPanel.setPreferredSize(METHOD_PANEL_SIZE);
		methodPanel.setSize(METHOD_PANEL_SIZE);
		commandPanel.add(methodPanel, BorderLayout.NORTH);

		//Upper part - drop down and button
		JPanel methodUpperPanel = new JPanel();
		methodUpperPanel.setBackground(Color.WHITE);
		methodPanel.add(methodUpperPanel, BorderLayout.NORTH);
		//Lower part - input fields
		JPanel methodLowerPanel = new JPanel();
		methodLowerPanel.setBackground(Color.WHITE);
		methodPanel.add(methodLowerPanel, BorderLayout.SOUTH);

		//Method choice
		final JComboBox methodBox = new JComboBox(ProxyMethodDescriptor.values());
		methodUpperPanel.add(methodBox);

		//Invoke button
		_invokeButton = new JButton("Invoke");
		_invokeButton.setEnabled(false);
		methodUpperPanel.add(_invokeButton);

		//Input
		final JTextField field = new JTextField();
		field.setBackground(methodPanel.getBackground());
		field.setColumns(16);
		methodLowerPanel.add(field);

		//Return count
		final JFormattedTextField nargoutField = new JFormattedTextField(NumberFormat.INTEGER_FIELD);
		nargoutField.setBackground(methodPanel.getBackground());
		nargoutField.setColumns(8);
		nargoutField.setBorder(BorderFactory.createTitledBorder("nargout"));
		methodLowerPanel.add(nargoutField);

		//Array entries
		final ArrayPanel arrayPanel = new ArrayPanel();
		methodLowerPanel.add(arrayPanel);

		//Method description
		final JEditorPane descriptionArea = new JEditorPane("text/html", "");
		descriptionArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		JScrollPane descriptionPane = new JScrollPane(descriptionArea);
		descriptionPane.setBackground(Color.WHITE);
		descriptionArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
		descriptionPane.setBorder(BorderFactory.createTitledBorder("Method description"));
		descriptionPane.setPreferredSize(DESCRIPTION_PANE_SIZE);
		descriptionArea.setEditable(false);
		commandPanel.add(descriptionPane, BorderLayout.SOUTH);

		//Listen for method choices
		methodBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//Get selected method
				ProxyMethodDescriptor method = (ProxyMethodDescriptor) methodBox.getSelectedItem();

				//Update box titles
				field.setBorder(BorderFactory.createTitledBorder(method.stringInputName));

				//Disable/enable return count and update text appropriately
				if (method.returnCountEnabled) {
					nargoutField.setText("1");
					nargoutField.setEnabled(true);
				} else {
					nargoutField.setText("N/A");
					nargoutField.setEnabled(false);
				}

				//Disable/enable array input appropriately
				arrayPanel.setBorder(BorderFactory.createTitledBorder(method.argsInputName));
				arrayPanel.enableInputFields(method.argsInputNumberEnabled);

				//Update description text
				descriptionArea.setText(method.message);
				//Scroll to the top of the description text
				descriptionArea.setCaretPosition(0);

				//Select text in field and switch focus
				field.selectAll();
				field.grabFocus();
			}
		});
		//Select first index to have action take effect
		methodBox.setSelectedIndex(0);

		//Invoke button action
		_invokeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ProxyMethodDescriptor descriptor = (ProxyMethodDescriptor) methodBox.getSelectedItem();

				//eval(String command)
				if (descriptor == ProxyMethodDescriptor.EVAL) {
					try {
						_proxyHolder.get().eval(field.getText());
						displayReturn();
					} catch (MatlabInvocationException ex) {
						displayException(ex);
					}
				}
				//returningEval(String command, int nargout)
				else if (descriptor == ProxyMethodDescriptor.RETURNING_EVAL) {
					int nargout = 0;
					try {
						nargout = Integer.parseInt(nargoutField.getText());
					} catch (Exception ex) {}

					try {
						displayResult(_proxyHolder.get().returningEval(field.getText(), nargout));
					} catch (MatlabInvocationException ex) {
						displayException(ex);
					}
				}
				//feval(String functionName, Object... args)
				else if (descriptor == ProxyMethodDescriptor.FEVAL) {
					try {
						_proxyHolder.get().feval(field.getText(), arrayPanel.getArray());
						displayReturn();
					} catch (MatlabInvocationException ex) {
						displayException(ex);
					}
				}
				//returningFeval(String functionName, int nargout, Object... args)
				else if (descriptor == ProxyMethodDescriptor.RETURNING_FEVAL) {
					int nargout = 0;
					try {
						nargout = Integer.parseInt(nargoutField.getText());
					} catch (Exception ex) {}

					try {
						displayResult(_proxyHolder.get().returningFeval(field.getText(), nargout, arrayPanel.getArray()));
					} catch (MatlabInvocationException ex) {
						displayException(ex);
					}

				}
				//setVariable(String variableName, Object value)
				else if (descriptor == ProxyMethodDescriptor.SET_VARIABLE) {
					try {
						_proxyHolder.get().setVariable(field.getText(), arrayPanel.getFirstEntry());
						displayReturn();
					} catch (MatlabInvocationException ex) {
						displayException(ex);
					}
				}
				//getVariable(String variableName)
				else if (descriptor == ProxyMethodDescriptor.GET_VARIABLE) {
					try {
						displayResult(_proxyHolder.get().getVariable(field.getText()));
					} catch (MatlabInvocationException ex) {
						displayException(ex);
					}
				}
			}
		});

		return commandPanel;
	}

	private void displayReturn() {
		_returnArea.setText("");
	}

	private void displayResult(Object result) {
		_returnPane.setBorder(BorderFactory.createTitledBorder(RETURNED_OBJECT));
		_returnArea.setForeground(Color.BLACK);
		_returnArea.setText(ReturnFormatter.formatResult(result));
		_returnArea.setCaretPosition(0);
	}

	private void displayException(Exception exc) {
		_returnPane.setBorder(BorderFactory.createTitledBorder(RETURNED_EXCEPTION));
		_returnArea.setForeground(Color.RED);
		_returnArea.setText(ReturnFormatter.formatException(exc));
		_returnArea.setCaretPosition(0);
	}
}
