/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.demo;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * The panel that contains the options to select elements of the array.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
@SuppressWarnings("serial")
class ArrayPanel extends JPanel {
	//Input options: String or Double
	private static final int DOUBLE_INDEX = 0, STRING_INDEX = 1;

	private static final String[] OPTIONS = new String[2];

	static {
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

	public ArrayPanel() {
		super(new GridLayout(NUM_ENTRIES, 2));
		this.setBackground(Color.WHITE);

		//Drop down lists and input fields
		_optionBoxes = new JComboBox[NUM_ENTRIES];
		_entryFields = new JTextField[NUM_ENTRIES];

		for (int i = 0; i < NUM_ENTRIES; i++) {
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
	public Object[] getArray() {
		ArrayList<Object> entries = new ArrayList<Object>();
		for (int i = 0; i < NUM_ENTRIES; i++) {
			if (!_entryFields[i].getText().isEmpty()) {
				if (_optionBoxes[i].getSelectedIndex() == DOUBLE_INDEX) {
					try {
						entries.add(Double.parseDouble(_entryFields[i].getText()));
					} catch (Exception e) {
						entries.add(0);
					}
				}
				if (_optionBoxes[i].getSelectedIndex() == STRING_INDEX) {
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
	public Object getFirstEntry() {
		if (!_entryFields[0].getText().isEmpty()) {
			if (_optionBoxes[0].getSelectedIndex() == DOUBLE_INDEX) {
				try {
					return Double.parseDouble(_entryFields[0].getText());
				} catch (Exception e) {
					return 0;
				}
			}
			if (_optionBoxes[0].getSelectedIndex() == STRING_INDEX) {
				return _entryFields[0].getText();
			}
		}

		return null;
	}

	/**
	 * Enable the first {@code n} fields for input. The others are disabled.
	 * 
	 * @param n
	 */
	public void enableInputFields(int n) {
		for (int i = 0; i < NUM_ENTRIES; i++) {
			_optionBoxes[i].setEnabled(i < n);
			_entryFields[i].setEnabled(i < n);
		}
	}
}
