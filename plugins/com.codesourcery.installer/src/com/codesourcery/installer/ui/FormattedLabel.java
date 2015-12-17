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

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Provides a label that can be formatted using certain HTML tags.  The tags
 * supported are:
 * <ul>
 * <li>&lt;b&gt;&lt;/b&gt; Bold text<li>
 * <li>&lt;i&gt;&lt;/i&gt; Italic text<li>
 * <li>&lt;u&gt;&lt;/u&gt; Underlined text<li>
 * <li>&lt;strike&gt;&lt;/strike&gt; Strikethrough text<li>
 * <li>&lt;small&gt;&lt;/small&gt; Smaller text<li>
 * <li>&lt;big&gt;&lt;/big&gt; Bigger text<li>
 * </ul>
 * 
 * Tags can be nested. i.e. specifying &lt;small&gt; twice will format the text
 * reduced in font size by 2.
 */
public class FormattedLabel extends Composite {
	/** Text for label */
	private String text;
	/** Label text */
	private StyledText labelText = null;
	/** Font cache */
	private HashMap<String, Font> fontCache = new HashMap<String, Font>();
	
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
		
		setFont(parent.getFont());
	}

	@Override
	public void dispose() {
		// Dispose of cached fonts
		for (Font font : fontCache.values()) {
			font.dispose();
		}
		fontCache.clear();
		
		super.dispose();
	}

	/**
	 * Sets the widget text.
	 * 
	 * @param text Text for the widget
	 */
	public void setText(String text) {
		this.text = text;
		
		setHtmlStyledText(labelText, text);
	}

	/**
	 * Get the widget text.
	 * 
	 * @return the Widget text
	 */
	public String getText() {
		return labelText.getText();
	}
	
	/**
	 * Sets the background color.
	 * 
	 * @param background Background color
	 */
	public void setBackground(Color background) {
		labelText.setBackground(background);
	}

	/**
	 * Sets the foreground color.
	 * 
	 * @param foreground Foreground color
	 */
	public void setForeground(Color foreground) {
		labelText.setForeground(foreground);
	}

	/**
	 * Returns a font from the cache or creates one.
	 * 
	 * @param fontSize Font size
	 * @param bold <code>true</code> for bold font
	 * @param italic <code>true</code> for italic font
	 * 
	 * @return Font
	 */
	private Font getFont(int fontSize, boolean bold, boolean italic) {
		// Font key
		StringBuffer buffer = new StringBuffer();
		buffer.append(Integer.toString(fontSize));
		buffer.append(Boolean.toString(bold));
		buffer.append(Boolean.toString(italic));
		String spec = buffer.toString();
		
		// Get font from cache
		Font font = fontCache.get(spec);
		// Create font if required and add to cache
		if (font == null) {
			FontData fontData = getFont().getFontData()[0];
			fontData.setHeight(fontSize);
			int style = SWT.NORMAL;
			if (bold) {
				style |= SWT.BOLD;
			}
			if (italic) {
				style |= SWT.ITALIC;
			}
			fontData.setStyle(style);
			
			font = new Font(getShell().getDisplay(), fontData);
			fontCache.put(spec, font);
		}
		
		return font;
	}
	
	/**
	 * Applies HTML styles from text to a StyledText widget.
	 *  
	 * @param label StyledText widget
	 * @param text Text with HTML formatting
	 */
	public void setHtmlStyledText(StyledText label, String text) {
		// set text label w/o style tags
		String labelText = text.replace("<b>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		labelText = labelText.replace("</b>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		labelText = labelText.replace("<i>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		labelText = labelText.replace("</i>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		labelText = labelText.replace("<u>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		labelText = labelText.replace("</u>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		labelText = labelText.replace("<strike>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		labelText = labelText.replace("</strike>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		labelText = labelText.replace("<small>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		labelText = labelText.replace("</small>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		labelText = labelText.replace("<big>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		labelText = labelText.replace("</big>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		label.setText(labelText);

		int length = labelText.length();
		if (length == 0)
			return;
		// Reset stles
		label.setStyleRange(new StyleRange(0, length - 1, null, null, SWT.NORMAL));
		
		FontData defaultFontData = getFont().getFontData()[0];
		int defaultFontSize = defaultFontData.getHeight();
		
		boolean bold = false;
		boolean italic = false;
		boolean underline = false;
		boolean strikethrough = false;
		int fontSize = defaultFontSize;
		int offset = 0;
		int start = -1;
		// Loop through formatted text
		for (int index = 0; index < text.length(); index ++) {
			// Start of tag
			if (text.charAt(index) == '<') {
				// End of tag
				int endTagIndex = text.indexOf('>', index);
				if (endTagIndex != -1) {
					// Get tag
					String tag = text.substring(index, endTagIndex + 1);

					if ((start != offset) && (start != -1)) {
						StyleRange styleRange = new StyleRange();
						styleRange.start = start;
						styleRange.length = offset - start;

						// If not default font, get the font
						if (fontSize != defaultFontSize) {
							styleRange.font = getFont(fontSize, bold, italic);
						}
						// Else just set the style
						else {
							styleRange.fontStyle = SWT.NORMAL;
							if (bold) {
								styleRange.fontStyle |= SWT.BOLD;
							}
							if (italic) {
								styleRange.fontStyle |= SWT.ITALIC;
							}
						}
						styleRange.underline = underline;
						styleRange.strikeout = strikethrough;

						label.setStyleRange(styleRange);
					}
					start = offset;
					
					// Bold
					if ("<b>".equalsIgnoreCase(tag)) {
						bold = true;
					}
					else if ("</b>".equalsIgnoreCase(tag)) {
						bold = false;
					}
					// Italic
					else if ("<i>".equalsIgnoreCase(tag)) {
						italic = true;
					}
					else if ("</i>".equalsIgnoreCase(tag)) {
						italic = false;
					}
					// Underline
					else if ("<u>".equalsIgnoreCase(tag)) {
						underline = true;
					}
					else if ("</u>".equalsIgnoreCase(tag)) {
						underline = false;
					}
					// Strike-through
					else if ("<strike>".equalsIgnoreCase(tag)) {
						strikethrough = true;
					}
					else if ("</strike>".equalsIgnoreCase(tag)) {
						strikethrough = false;
					}
					// Smaller text
					else if ("<small>".equalsIgnoreCase(tag)) {
						if (fontSize > 1) {
							fontSize --;
						}
					}
					else if ("</small>".equalsIgnoreCase(tag)) {
						fontSize ++;
					}
					// Larger text
					else if ("<big>".equalsIgnoreCase(tag)) {
						fontSize ++;
					}
					else if ("</big>".equalsIgnoreCase(tag)) {
						if (fontSize > 1) {
							fontSize --;
						}
					}
					
					index += tag.length() - 1;
				}
			}
			else {
				offset ++;
			}
		}
	}

	@Override
	public void setFont(Font font) {
		super.setFont(font);
		
		labelText.setFont(font);
		if (text != null) {
			setText(text);
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
