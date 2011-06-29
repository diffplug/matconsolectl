package demo.gui;

/*
 * Copyright (c) 2011, Joshua Kaplan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *  - Neither the name of matlabcontrol nor the names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;

import javax.imageio.ImageIO;
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
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;
import matlabcontrol.PermissiveSecurityManager;
import matlabcontrol.extensions.MatlabCallbackInteractor;
import matlabcontrol.extensions.MatlabCallbackInteractor.MatlabCallback;
import matlabcontrol.extensions.MatlabCallbackInteractor.MatlabDataCallback;

/**
 * A GUI example to demonstrate the main functionality of controlling MATLAB with matlabcontrol.
 * <br><br>
 * The icon is part of the Um collection created by <a href="mailto:mattahan@gmail.com">mattahan (Paul Davey)</a>. It is
 * licensed under the <a href="http://creativecommons.org/licenses/by-nc-sa/3.0/">CC Attribution-Noncommercial-Share
 * Alike 3.0 License</a>.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
@SuppressWarnings("serial")
public class DemoFrame extends JFrame
{    
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
    private static final int PANEL_WIDTH = 600;
    private static final Dimension CONNECTION_PANEL_SIZE = new Dimension(PANEL_WIDTH, 70),
                                   RETURN_PANEL_SIZE = new Dimension(PANEL_WIDTH, 250),
                                   METHOD_PANEL_SIZE = new Dimension(PANEL_WIDTH, 110 + 28 * ArrayPanel.NUM_ENTRIES),
                                   DESCRIPTION_PANE_SIZE = new Dimension(PANEL_WIDTH, 200),
                                   COMMAND_PANEL_SIZE = new Dimension(PANEL_WIDTH, METHOD_PANEL_SIZE.height +
                                                                                   DESCRIPTION_PANE_SIZE.height),
                                   MAIN_PANEL_SIZE = new Dimension(PANEL_WIDTH, CONNECTION_PANEL_SIZE.height +
                                                                                COMMAND_PANEL_SIZE.height + 
                                                                                RETURN_PANEL_SIZE.height);
    //Factory to create proxy
    private MatlabProxyFactory _factory;
    
    //Proxy to communicate with MATLAB
    private MatlabCallbackInteractor _interactor;
    
    //UI components
    private JButton _invokeButton;
    private JScrollPane _returnPane;
    private JTextArea _returnArea;
    
    /**
     * Create the main GUI.
     */
    public DemoFrame(String title)
    {
        super(title);
        
        System.setSecurityManager(new PermissiveSecurityManager());
        
        try
        {
            this.setIconImage(ImageIO.read(this.getClass().getResource("/demo/gui/icon.png")));
        }
        catch(Exception e) { }
        
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
                .build();
        _factory = new MatlabProxyFactory(options);

        //Connect to MATLAB when the Connect button is pressed
        connectionButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    //Request a proxy
                    _factory.requestProxy(new MatlabProxyFactory.RequestCallback()
                    {
                        @Override
                        public void proxyCreated(final MatlabProxy proxy)
                        {
                            _interactor = new MatlabCallbackInteractor(proxy);
                        
                            proxy.addDisconnectionListener(new MatlabProxy.DisconnectionListener()
                            {
                                @Override
                                public void proxyDisconnected(MatlabProxy proxy)
                                {
                                    _interactor = null;
    
                                    EventQueue.invokeLater(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
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
                            EventQueue.invokeLater(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    String status;
                                    if(proxy.isExistingSession())
                                    {
                                        status = STATUS_CONNECTED_EXISTING;
                                    }
                                    else
                                    {
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
                }
                catch(MatlabConnectionException exc)
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
             * On window appearance set the minimum size to the same width and 80% of the height.
             */
            @Override
            public void windowOpened(WindowEvent e)
            {
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
            @Override
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
            @Override
            public void actionPerformed(ActionEvent e)
            {
                //eval(String command)
                if(methodBox.getSelectedIndex() == MethodDescriptor.EVAL_INDEX)
                {
                    _invokeButton.setEnabled(false);
                    
                    _interactor.eval(field.getText(), new MatlabCallback()
                    {
                        @Override
                        public void invocationSucceeded()
                        {
                            displayReturn();
                            _invokeButton.setEnabled(true);
                        }

                        @Override
                        public void invocationFailed(MatlabInvocationException ex)
                        {
                            displayException(ex);
                            _invokeButton.setEnabled(true);
                        }
                    });
                }
                //returningEval(String command, int returnCount)
                else if(methodBox.getSelectedIndex() == MethodDescriptor.RETURNING_EVAL_INDEX)
                {
                    _invokeButton.setEnabled(false);
                    
                    int returnCount = 0;
                    try
                    {
                        returnCount = Integer.parseInt(returnCountField.getText());
                    }
                    catch(Exception ex) { }
                    
                    _interactor.returningEval(field.getText(), returnCount, new MatlabDataCallback()
                    {
                        @Override
                        public void invocationSucceeded(Object data)
                        {
                            displayResult(data);
                            _invokeButton.setEnabled(true);
                        }

                        @Override
                        public void invocationFailed(MatlabInvocationException ex)
                        {
                            displayException(ex);
                            _invokeButton.setEnabled(true);
                        }
                    });
                }
                //feval(String functionName, Object[] args)
                else if(methodBox.getSelectedIndex() == MethodDescriptor.FEVAL_INDEX)
                {
                    _invokeButton.setEnabled(false);
                    
                    _interactor.feval(field.getText(), arrayPanel.getArray(), new MatlabCallback()
                    {
                        @Override
                        public void invocationSucceeded()
                        {
                            displayReturn();
                            _invokeButton.setEnabled(true);
                        }

                        @Override
                        public void invocationFailed(MatlabInvocationException ex)
                        {
                            displayException(ex);
                            _invokeButton.setEnabled(true);
                        }
                    });
                }
                //returningFeval(String functionName, Object[] args)
                else if(methodBox.getSelectedIndex() == MethodDescriptor.RETURNING_AUTO_FEVAL_INDEX)
                {
                    _invokeButton.setEnabled(false);
                            
                    _interactor.returningFeval(field.getText(), arrayPanel.getArray(), new MatlabDataCallback()
                    {
                        @Override
                        public void invocationSucceeded(Object data)
                        {
                            displayResult(data);
                            _invokeButton.setEnabled(true);
                        }

                        @Override
                        public void invocationFailed(MatlabInvocationException ex)
                        {
                            displayException(ex);
                            _invokeButton.setEnabled(true);
                        }
                    });
                }
                //returningFeval(String functionName, Object[] args, int returnCount)
                else if(methodBox.getSelectedIndex() == MethodDescriptor.RETURNING_FEVAL_INDEX)
                {
                    _invokeButton.setEnabled(false);
                    
                    int returnCount = 0;
                    try
                    {
                        returnCount = Integer.parseInt(returnCountField.getText());
                    }
                    catch(Exception ex) { }
                            
                    _interactor.returningFeval(field.getText(), arrayPanel.getArray(), returnCount, new MatlabDataCallback()
                    {
                        @Override
                        public void invocationSucceeded(Object data)
                        {
                            displayResult(data);
                            _invokeButton.setEnabled(true);
                        }

                        @Override
                        public void invocationFailed(MatlabInvocationException ex)
                        {
                            displayException(ex);
                            _invokeButton.setEnabled(true);
                        }
                    });
                }
                //setVariable(String variableName, Object value)
                else if(methodBox.getSelectedIndex() == MethodDescriptor.SET_VARIABLE_INDEX)
                {
                    _invokeButton.setEnabled(false);
                    
                    _interactor.setVariable(field.getText(), arrayPanel.getFirstEntry(), new MatlabCallback()
                    {
                        @Override
                        public void invocationSucceeded()
                        {
                            displayReturn();
                            _invokeButton.setEnabled(true);
                        }

                        @Override
                        public void invocationFailed(MatlabInvocationException ex)
                        {
                            displayException(ex);
                            _invokeButton.setEnabled(true);
                        }
                    });
                }
                //getVariable(String variableName)
                else if(methodBox.getSelectedIndex() == MethodDescriptor.GET_VARIABLE_INDEX)
                {
                    _invokeButton.setEnabled(false);
                            
                    _interactor.getVariable(field.getText(), new MatlabDataCallback()
                    {
                        @Override
                        public void invocationSucceeded(Object data)
                        {
                            displayResult(data);
                            _invokeButton.setEnabled(true);
                        }

                        @Override
                        public void invocationFailed(MatlabInvocationException ex)
                        {
                            displayException(ex);
                            _invokeButton.setEnabled(true);
                        }
                    });
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
}