/*******************************************************************************
 *  Copyright (c) 2014 Mentor Graphics and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Mentor Graphics - initial API and implementation
 *******************************************************************************/
package com.codesourcery.installer.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Provides a label that can be formatted with normal, bold, and italic
 * text.
 */
public class FormattedLabel extends Composite {
	/** Label text */
	private StyledText labelText = null;

	/**
	 * Constructor
	 * 
	 * @param parent Parent of widget
	 * @param style style
	 */
	public FormattedLabel(Composite parent, int style) {
		super(parent, SWT.NONE);

		setLayout(new FillLayout());

		// Force read only for label
		labelText = new StyledText(this, style | SWT.READ_ONLY);

		labelText.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		labelText.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

		labelText.setEnabled(false);
		labelText.setEditable(false);
	}

	/**
	 * Sets the widget text
	 * Text enclosed with a <b> and </b> will be bolded
	 * 
	 * @param text
	 */
	public void setText(String text) {
		setHtmlStyledText(labelText, text);
	}

	/**
	 * Get the widget text
	 * 
	 * @return the Widget text
	 */
	public String getText() {
		return labelText.getText();
	}

	/**
	 * Applies HTML styles from text to a StyledText widget.
	 *  
	 * @param label StyledText widget
	 * @param text Text with HTML formatting
	 */
	public static void setHtmlStyledText(StyledText label, String text) {
		// set text label w/o style tags
		String displayText = text;
		displayText = displayText.replaceAll("<b>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		displayText = displayText.replaceAll("</b>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		displayText = displayText.replaceAll("<i>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		displayText = displayText.replaceAll("</i>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		label.setText(displayText);

		// Reset styles
		int endRange = label.getCharCount() - 1;
		if (endRange < 0)
			endRange = 0;
		label.setStyleRange(new StyleRange(0, endRange, null, null, SWT.NORMAL));

		int index = 0;
		int styleCount = -1;
		int startIndex = 0;
		int style = SWT.NONE;
		StringBuffer buffer = new StringBuffer(text);
		for (int i1 = 0; i1 < buffer.length() - 3; i1++) {
			if (buffer.charAt(i1) == '<') {
				String currentTag = buffer.substring(i1, i1 + 3);
				if (currentTag.equals("<b>")) { //$NON-NLS-1$
					startIndex = index;
					i1 += 2;
					styleCount = 0;
					style = SWT.BOLD;
				}
				if (currentTag.equals("<i>")) { //$NON-NLS-1$
					startIndex = index;
					i1 += 2;
					styleCount = 0;
					style = SWT.ITALIC;
				}
				// only 3 chars in comparison below
				else if (currentTag.equals("</b") || currentTag.equals("</i")) {  //$NON-NLS-1$//$NON-NLS-2$
					label.setStyleRange(new StyleRange(startIndex, styleCount, null, null, style));
					i1 += 3;
					styleCount = -1;
				}
			} else {
				if (styleCount != -1)
					styleCount++;

				index++;
			}
		}
	}

	/** @see org.eclipse.swt.widgets.Control#setEnabled(boolean) */
	public void setEnabled(boolean enabled) {
		labelText.setEnabled(enabled);
	}

	/** @see org.eclipse.swt.custom.StyledText#setWordWrap(boolean) */
	public void setWordWrap(boolean wrap) {
		labelText.setWordWrap(wrap);
	}
}
