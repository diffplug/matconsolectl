package example.remote;

/*
 * Copyright (c) 2010, Joshua Kaplan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of matlabcontrol nor the names of its contributors may
 *    be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabConnectionListener;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.RemoteMatlabProxy;
import matlabcontrol.RemoteMatlabProxyFactory;

/**
 * A GUI example to demonstrate the main functionality of controlling
 * a remote session with matlabcontrol.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
@SuppressWarnings("serial")
class RemoteMain extends JFrame
{	
	//Status messages
	private static final String STATUS_DISCONNECTED = "Connection Status: Disconnected",
								STATUS_CONNECTING = "Connection Status: Connecting",
								STATUS_CONNECTED = "Connection Status: Connected";
	
	//Return messages
	private static final String RETURNED_DEFAULT = "Returned Object / Java Exception",
								RETURNED_OBJECT = "Returned Object",
								RETURNED_EXCEPTION = "Java Exception";
	
	//Panel/Pane sizes
	private static final int WIDTH = 600;
	private static final Dimension CONNECTION_PANEL_SIZE = new Dimension(WIDTH, 70),
								   RETURN_PANEL_SIZE = new Dimension(WIDTH, 250),
								   METHOD_PANEL_SIZE = new Dimension(WIDTH, 110 + 28 * ArrayPanel.NUM_ENTRIES),
								   DESCRIPTION_PANE_SIZE = new Dimension(WIDTH, 200),
								   COMMAND_PANEL_SIZE = new Dimension(WIDTH, METHOD_PANEL_SIZE.height +
										   										   DESCRIPTION_PANE_SIZE.height),
								   MAIN_PANEL_SIZE = new Dimension(WIDTH, CONNECTION_PANEL_SIZE.height +
										   										COMMAND_PANEL_SIZE.height + 
										   										RETURN_PANEL_SIZE.height);
	//Factory to create proxy
	private RemoteMatlabProxyFactory _factory;
	
	//Proxy to communicate with MATLAB
	private RemoteMatlabProxy _proxy;
	
	//UI components
	private JButton _invokeButton;
	private JScrollPane _returnPane;
	private JTextArea _returnArea;
	
	/**
	 * Create the main GUI.
	 */
	private RemoteMain()
	{
		super("Remote matlabcontrol Example");
		
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
		
		//Attempt to create proxy factory
		try
		{	
			_factory = new RemoteMatlabProxyFactory();
		}
		catch(MatlabConnectionException e)
		{
			displayException(e);
			
			_invokeButton.setEnabled(false);
			connectionButton.setEnabled(false);
		}
		//If the factory was created, add a connection listener for when a proxy is received
		if(_factory != null)
		{
			_factory.addConnectionListener(new MatlabConnectionListener()
			{
				//When the connection is established, store the proxy, update UI
				public void connectionEstablished(RemoteMatlabProxy proxy)
				{
					_proxy = proxy;
					
					connectionPanel.setBorder(BorderFactory.createTitledBorder(STATUS_CONNECTED));
					connectionBar.setValue(100);
					connectionBar.setIndeterminate(false);
					_invokeButton.setEnabled(true);
				}
	
				//When the connection is lost, null the proxy, update UI
				public void connectionLost(RemoteMatlabProxy proxy)
				{
					_proxy = null;
	
					connectionPanel.setBorder(BorderFactory.createTitledBorder(STATUS_DISCONNECTED));
					_returnPane.setBorder(BorderFactory.createTitledBorder(RETURNED_DEFAULT));
					_returnArea.setText("");
					connectionBar.setValue(0);
					connectionButton.setEnabled(true);
					_invokeButton.setEnabled(false);
				}
			});
		}

		//Connect to Matlab when the Connect button is pressed
		connectionButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					//Request a proxy, to be received by MatlabConnectionListener
					_factory.requestProxy();
					
					//Update GUI
					connectionPanel.setBorder(BorderFactory.createTitledBorder(STATUS_CONNECTING));
					connectionBar.setIndeterminate(true);
					connectionButton.setEnabled(false);
				}
				catch (MatlabConnectionException exc)
				{
					_returnPane.setBorder(BorderFactory.createTitledBorder(RETURNED_EXCEPTION));
					_returnArea.setText(ReturnFormatter.formatException(exc));
					_returnArea.setCaretPosition(0);
				}
			}
		});
		
		//Window events
		this.addWindowListener(new WindowAdapter()
		{
			/**
			 * On window appearance set the minimum size to the same width
			 * and 80% of the height.
			 */
			public void windowOpened(WindowEvent e)
			{
				Dimension size = RemoteMain.this.getSize();
				size.height *= .8;
				RemoteMain.this.setMinimumSize(size);
			}
			
			/**
			 * When this window closes, if MATLAB is open, close it
			 */
			public void windowClosing(WindowEvent e)
			{
				if(_proxy != null && _proxy.isConnected())
				{
					try
					{
						_proxy.exit();
					}
					catch (MatlabInvocationException exc) { }
				}	
			}
		});
		
		//Display frame
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.pack();
		this.setVisible(true);
	}

	/**
	 * Create the command panel.
	 * 
	 * @param returnArea
	 * @return
	 */
	private JPanel createCommandPanel()
	{
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
		final JComboBox methodBox = new JComboBox(MethodDescriptor.METHODS);
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
		final JFormattedTextField returnCountField = new JFormattedTextField(NumberFormat.INTEGER_FIELD);
		returnCountField.setBackground(methodPanel.getBackground());
		returnCountField.setColumns(8);
		returnCountField.setBorder(BorderFactory.createTitledBorder("returnCount"));
		methodLowerPanel.add(returnCountField);
		
		//Array entries
		final ArrayPanel arrayPanel = new ArrayPanel();
		methodLowerPanel.add(arrayPanel);
		
		//Method description
		final JTextArea descriptionArea = new JTextArea();
		JScrollPane descriptionPane  = new JScrollPane(descriptionArea);
		descriptionPane.setBackground(Color.WHITE);
		descriptionPane.setBorder(BorderFactory.createTitledBorder("Method description"));
		descriptionPane.setPreferredSize(DESCRIPTION_PANE_SIZE);
		descriptionArea.setWrapStyleWord(true);
		descriptionArea.setLineWrap(true);
		descriptionArea.setEditable(false);
		commandPanel.add(descriptionPane, BorderLayout.SOUTH);
		
		//Listen for method choices
		methodBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				//Get selected method
				MethodDescriptor method = (MethodDescriptor) methodBox.getSelectedItem();
				
				//Update box titles
				field.setBorder(BorderFactory.createTitledBorder(method.stringInputName));
				
				//Disable/enable return count and update text appropriately
				if(method.returnCountEnabled)
				{
					returnCountField.setText("1");
					returnCountField.setEnabled(true);
				}
				else
				{
					returnCountField.setText("N/A");
					returnCountField.setEnabled(false);
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
		_invokeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				//eval(String command)
				if(methodBox.getSelectedIndex() == MethodDescriptor.EVAL_INDEX)
				{
					try
					{
						_proxy.eval(field.getText());
						displayReturn();
					}
					catch(Exception exc)
					{
						displayException(exc);
					}
				}
				//returningEval(String command, int returnCount)
				else if(methodBox.getSelectedIndex() == MethodDescriptor.RETURNING_EVAL_INDEX)
				{
					try
					{
						Object result = _proxy.returningEval(field.getText(), Integer.parseInt(returnCountField.getText()));					
						displayResult(result);
					}
					catch(Exception exc)
					{
						displayException(exc);
					}
				}
				//feval(String functionName, Object[] args)
				else if(methodBox.getSelectedIndex() == MethodDescriptor.FEVAL_INDEX)
				{
					try
					{
						_proxy.feval(field.getText(), arrayPanel.getArray());
						displayReturn();
					}
					catch(Exception exc)
					{
						displayException(exc);
					}
				}
				//returningFeval(String functionName, Object[] args)
				else if(methodBox.getSelectedIndex() == MethodDescriptor.RETURNING_AUTO_FEVAL_INDEX)
				{
					try
					{
						Object result = _proxy.returningFeval(field.getText(), arrayPanel.getArray());
						displayResult(result);
					}
					catch(Exception exc)
					{
						displayException(exc);
					}
				}
				//returningFeval(String functionName, Object[] args, int returnCount)
				else if(methodBox.getSelectedIndex() == MethodDescriptor.RETURNING_FEVAL_INDEX)
				{
					try
					{
						Object result = _proxy.returningFeval(field.getText(), arrayPanel.getArray(), Integer.parseInt(returnCountField.getText()));
						displayResult(result);
					}
					catch(Exception exc)
					{
						displayException(exc);
					}
				}
				//setVariable(String variableName, Object value)
				else if(methodBox.getSelectedIndex() == MethodDescriptor.SET_VARIABLE_INDEX)
				{
					try
					{
						_proxy.setVariable(field.getText(), arrayPanel.getFirstEntry());
						displayReturn();
					}
					catch(Exception exc)
					{
						displayException(exc);
					}
				}
				//getVariable(String variableName)
				else if(methodBox.getSelectedIndex() == MethodDescriptor.GET_VARIABLE_INDEX)
				{
					try
					{
						Object result = _proxy.getVariable(field.getText());
						displayResult(result);
					}
					catch(Exception exc)
					{
						displayException(exc);
					}
				}
			}
		});
		
		return commandPanel;
	}
	
	private void displayReturn()
	{
		_returnArea.setText("");
	}
	
	private void displayResult(Object result)
	{
		_returnPane.setBorder(BorderFactory.createTitledBorder(RETURNED_OBJECT));
		_returnArea.setForeground(Color.BLACK);
		_returnArea.setText(ReturnFormatter.formatResult(result));
		_returnArea.setCaretPosition(0);
	}
	
	private void displayException(Exception exc)
	{
		_returnPane.setBorder(BorderFactory.createTitledBorder(RETURNED_EXCEPTION));
		_returnArea.setForeground(Color.RED);
		_returnArea.setText(ReturnFormatter.formatException(exc));
		_returnArea.setCaretPosition(0);
	}
	
	/**
	 * Gets everything started.
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		new RemoteMain();
	}
}