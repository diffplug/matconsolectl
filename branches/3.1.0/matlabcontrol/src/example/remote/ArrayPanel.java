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

import java.awt.Color;
import java.awt.GridLayout;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * The panel that contains the options to select elements of the array.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
@SuppressWarnings("serial")
class ArrayPanel extends JPanel
{
	//Input options: String or Double
	private static final int DOUBLE_INDEX = 0,
							 STRING_INDEX = 1;
	private static final String[] OPTIONS = new String[2];
	static
	{
		OPTIONS[DOUBLE_INDEX] = "Double";
		OPTIONS[STRING_INDEX] = "String";
	}
	
	/**
	 * Number of fields and drop down lists.
	 */
	public static final int NUM_ENTRIES = 3;
	
	/**
	 * Drop down lists to choose between object types.
	 */
	private final JComboBox[] _optionBoxes;
	
	/**
	 * Fields for inputting values.
	 */
	private final JTextField[] _entryFields;
	
	public ArrayPanel()
	{
		super(new GridLayout(NUM_ENTRIES,2));
		this.setBackground(Color.WHITE);
		
		//Drop down lists and input fields
		_optionBoxes = new JComboBox[NUM_ENTRIES];
		_entryFields = new JTextField[NUM_ENTRIES];
		
		for(int i = 0; i < NUM_ENTRIES; i++)
		{
			_optionBoxes[i] = new JComboBox(OPTIONS);
			_entryFields[i] = new JTextField(8);
			this.add(_optionBoxes[i]);
			this.add(_entryFields[i]);
		}
	}
	
	/**
	 * Take the elements of the fields and put them into an array.
	 * 
	 * @return
	 */
	public Object[] getArray()
	{
		Vector<Object> entries = new Vector<Object>();
		for(int i = 0; i < NUM_ENTRIES; i++)
		{
			if(!_entryFields[i].getText().isEmpty())
			{
				if(_optionBoxes[i].getSelectedIndex() == DOUBLE_INDEX)
				{
					entries.add(Double.parseDouble(_entryFields[i].getText()));
				}
				if(_optionBoxes[i].getSelectedIndex() == STRING_INDEX)
				{
					entries.add(_entryFields[i].getText());
				}
			}
		}
		
		return entries.toArray();
	}
	
	/**
	 * Return the first entry of the fields.
	 * 
	 * @return
	 */
	public Object getFirstEntry()
	{
		if(!_entryFields[0].getText().isEmpty())
		{
			if(_optionBoxes[0].getSelectedIndex() == DOUBLE_INDEX)
			{
				return Double.parseDouble(_entryFields[0].getText());
			}
			if(_optionBoxes[0].getSelectedIndex() == STRING_INDEX)
			{
				return _entryFields[0].getText();
			}
		}
		
		return null;
	}
	
	/**
	 * Enable the first <code>n</code> fields for input.
	 * The others are disabled.
	 * 
	 * @param n
	 */
	public void enableInputFields(int n)
	{
		for(int i = 0; i < NUM_ENTRIES; i++)
		{
			_optionBoxes[i].setEnabled(i < n);
			_entryFields[i].setEnabled(i < n);
		}
	}
}