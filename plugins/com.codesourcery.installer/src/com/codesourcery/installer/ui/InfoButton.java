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

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

/**
 * A button that supports an image, label text, and/or wrapped description
 * text.
 */
public class InfoButton extends Canvas {
	/** Text draw flags */
	private static int DRAW_FLAGS = SWT.DRAW_MNEMONIC | SWT.DRAW_TAB | SWT.DRAW_TRANSPARENT | SWT.DRAW_DELIMITER;
	/** Shortened text replacement */
	private static final String ELLIPSIS = " ... "; //$NON-NLS-1$

	/** Element colors */
	public enum ElementColor {
		/** Color of label text */
		label,
		/** Color of label text when label is selected */
		selectedLabel,
		/** Color of description text */
		description,
		/** Color of description text when label is selected */ 
		selectedDescription,
		/** Hover background color */
		hoverBackground,
		/** Select background color */
		selectBackground
	}

	/** Size of rectangle rounding */
	private static final int ROUND_SIZE = 6;
	
	/** Focus line dash style */
	private int[] FOCUS_LINE_STYLE = new int[] {1, 1};
	
	/** Label image */
	private Image image;
	/** Disabled image */
	private Image disabledImage;
	/** Label text */
	private String text;
	/** Label description */
	private String description;
	/** Text layout */
	private TextLayout textLayout;
	/** Margin between image and label text */
	private int textMargin = 10;
	/** Horizontal margin */
	private int itemHorizontalMargin = 4;
	/** Vertical margin */
	private int itemVerticalMargin = 4;
	/** <code>true</code> if item is currently tracking */
	private boolean tracking = false;
	/** Element colors */
	private Color[] colors = new Color[ElementColor.values().length];
	/** Label font */
	private Font labelFont;
	/** Description font */
	private Font descriptionFont;
	/** <code>true</code> if selected */
	private boolean selected;
	/** <code>true</code> if has focus */
	private boolean hasFocus = false;
	/** Selection listeners */
	private ListenerList selectionListeners;
	/** <code>true</code> to draw rounded selection */
	private boolean rounded = false;
	/** <code>true</code> if button label should be shortened */
	private boolean shortenText = false;
	
	/**
	 * Constructor
	 * 
	 * @param parent Parent composite
	 * @param style Style flags
	 * @param image Image or <code>null</code>
	 * @param text Text or <code>null</code>
	 * @param description Description or <code>null</code>
	 */
	public InfoButton(Composite parent, int style, Image image, 
			String text, String description) {
		this(parent, style);
		
		setImage(image);
		setText(text);
		setDescription(description);
	}

	/**
	 * Constructor
	 * 
	 * The following style flags are supported:
	 * SWT.TOGGLE, SWT.RADIO, SWT.NO_FOCUS, SWT.READ_ONLY
	 * 
	 * @param parent Parent composite
	 * @param style Style flags
	 */
	public InfoButton(Composite parent, int style) {
		super(parent, style);
		
		// Create text layout
		textLayout = new TextLayout(getShell().getDisplay());
		// Initialize default colors
		initDefaultColors();
		
		// Add paint listener
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				onPaint(e);
			}
		});

		// Add mouse listener
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				select(true);
			}

			@Override
			public void mouseUp(MouseEvent e) {
				select(false);
			}
		});
		
		// Add mouse track listener
		addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseEnter(MouseEvent e) {
				tracking = true;
				if (canUpdate()) {
					redraw();
				}
			}
			
			@Override
			public void mouseExit(MouseEvent e) {
				tracking = false;
				if (canUpdate()) {
					redraw();
				}
			}
		});
		// Add focus listener
		addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
					if (canUpdate()) {
					if (!tracking && ((getStyle() & SWT.NO_FOCUS) != SWT.NO_FOCUS)) {
						if (!getSelection()) {
							hasFocus = true;
							redraw();
						}
					}
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (canUpdate()) {
					if ((getStyle() & SWT.NO_FOCUS) != SWT.NO_FOCUS) {
						hasFocus = false;
						redraw();
					}
				}
			}
		});
		// Handle focus traversal
		addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				e.doit = true;
			}
		});
		// Add key listener
		addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (canUpdate()) {
					// Arrow down - increase the focus item
					if (e.keyCode == SWT.ARROW_DOWN) {
						selectNext(true);
					}
					// Arrow up - decrease the focus item
					else if (e.keyCode == SWT.ARROW_UP) {
						selectNext(false);
					}
					// Space - select item
					else if (e.keyCode == SWT.SPACE) {
						select(true);
					}
				}
				e.doit = true;
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.SPACE){
					select(false);
				}
				e.doit = true;
			}
		});
		
		// Set label & description font
		setFont(parent.getFont());
	}

	/**
	 * Selects the next or previous button in a radio group.
	 * 
	 * @param next <code>true</code> to select next button, <code>false</code> to
	 * select previous button
	 */
	private void selectNext(boolean next) {
		Control[] children = getParent().getChildren();
		for (int index = 0; index < children.length; index ++) {
			if (this == children[index]) {
				InfoButton nextButton = null;
				if (next) {
					if ((index + 1 < children.length) && (children[index] instanceof InfoButton)) {
						nextButton = (InfoButton)children[index + 1];
					}
				}
				else {
					if ((index - 1 >= 0) && (children[index - 1] instanceof InfoButton)) {
						nextButton = (InfoButton)children[index - 1];
					}
				}
	
				if ((nextButton != null) && ((getStyle() & SWT.RADIO) == SWT.RADIO)) {
					nextButton.setFocus();
					nextButton.select(true);
					break;
				}
			}
		}
	}

	@Override
	public void dispose() {
		if (textLayout != null) {
			textLayout.dispose();
			textLayout = null;
		}
		if (disabledImage != null) {
			disabledImage.dispose();
			disabledImage = null;
		}
		super.dispose();
	}

	/**
	 * Returns if the button can update.
	 * 
	 * @return <code>true</code> if button is can update
	 */
	private boolean canUpdate() {
		return (isEnabled() && ((getStyle() & SWT.READ_ONLY) != SWT.READ_ONLY));
	}
	
	/**
	 * Initializes default colors
	 */
	private void initDefaultColors() {
		setColor(ElementColor.label, getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
		setColor(ElementColor.selectedLabel, getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
		setColor(ElementColor.description, getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
		setColor(ElementColor.selectedDescription, getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
		setColor(ElementColor.hoverBackground, getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
		setColor(ElementColor.selectBackground, getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
	}

	/**
	 * Sets if the label text should be shortened if it will not fit in the
	 * button width.  The middle of the text will be replaced with "..." to
	 * fit the width.
	 *  
	 * @param shortenText <code>true</code> to shorten text
	 */
	public void setShortenText(boolean shortenText) {
		this.shortenText = shortenText;
	}
	
	/**
	 * Returns if the label text should be shortened.
	 * 
	 * @return <code>true</code> if text will be shortened
	 */
	public boolean getShortenText() {
		return shortenText;
	}

	/**
	 * Sets rounded highlight.
	 * 
	 * @param rounded <code>true</code> to draw rounded highlight
	 */
	public void setRounded(boolean rounded) {
		this.rounded = rounded;
	}
	
	/**
	 * Gets rounded highlight.
	 * 
	 * @return <code>true</code> to draw rounded highlight
	 */
	public boolean getRounded() {
		return rounded;
	}
	
	/**
	 * Sets the label and description font.
	 * 
	 * @param font Font
	 */
	public void setFont(Font font) {
		super.setFont(font);
		
		this.labelFont = font;
		this.descriptionFont = font;
		redraw();
	}
	
	/**
	 * Sets the label font.
	 * 
	 * @param font Label font
	 */
	public void setLabelFont(Font font) {
		this.labelFont = font;
		redraw();
	}
	
	/**
	 * Returns the label font.
	 * 
	 * @return Label font
	 */
	public Font getLabelFont() {
		return labelFont;
	}
	
	/**
	 * Sets the description font.
	 * 
	 * @param font Description font
	 */
	public void setDescriptionFont(Font font) {
		this.descriptionFont = font;
		redraw();
	}
	
	/**
	 * Returns the description font.
	 * 
	 * @return Description font
	 */
	public Font getDescriptionFont() {
		return descriptionFont;
	}
	
	/**
	 * Sets an element color.
	 * 
	 * @param element Element
	 * @param color Color
	 */
	public void setColor(ElementColor element, Color color) {
		colors[element.ordinal()] = color;
		redraw();
	}
	
	/**
	 * Returns an element color.
	 * 
	 * @param element Element
	 * @return Color
	 */
	public Color getColor(ElementColor element) {
		return colors[element.ordinal()];
	}

	/**
	 * Sets the image.
	 * 
	 * @param image Image or <code>null</code>
	 */
	public void setImage(Image image) {
		this.image = image;
		if (disabledImage != null) {
			disabledImage.dispose();
		}
		disabledImage = (image != null) ? new Image(getShell().getDisplay(), image, SWT.IMAGE_GRAY) : null;
		redraw();
	}
	
	/**
	 * Returns the image.
	 * 
	 * @return Image or <code>null</code>
	 */
	public Image getImage() {
		return image;
	}

	/**
	 * Sets the text.
	 * 
	 * @param text Text or <code>null</code>
	 */
	public void setText(String text) {
		this.text = text;
		redraw();
	}
	
	/**
	 * Returns the text.
	 * 
	 * @return Text or <code>null</code>
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * Sets the description.
	 * 
	 * @param description Description or <code>null</code>
	 */
	public void setDescription(String description) {
		this.description = description;
		redraw();
	}
	
	/**
	 * Returns the description.
	 * 
	 * @return Description or <code>null</code>
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the margin between the image and text.
	 * 
	 * @param textMargin Text margin
	 */
	public void setTextMargin(int textMargin) {
		this.textMargin = textMargin;
		redraw();
	}
	
	/**
	 * Returns the margin between the image and text.
	 * 
	 * @return Text margin
	 */
	public int getTextMargin() {
		return textMargin;
	}
	
	/**
	 * Sets the horizontal margin.
	 * 
	 * @param itemHorizontalMargin Horizontal margin
	 */
	public void setHorizontalMargin(int itemHorizontalMargin) {
		this.itemHorizontalMargin = itemHorizontalMargin;
		redraw();
	}
	
	/**
	 * Returns the horizontal margin.
	 * 
	 * @return Horizontal margin
	 */
	public int getHorizontalMargin() {
		return itemHorizontalMargin;
	}
	
	/**
	 * Sets the vertical margin.
	 * 
	 * @param itemVerticalMargin Vertical margin
	 */
	public void setVerticalMargin(int itemVerticalMargin) {
		this.itemVerticalMargin = itemVerticalMargin;
		redraw();
	}
	
	/**
	 * Returns the vertical margin.
	 * 
	 * @return Vertical margin
	 */
	public int getVerticalMargin() {
		return itemVerticalMargin;
	}
	
	/**
	 * Sets the selection.
	 * 
	 * @param selected <code>true</code> if selected
	 */
	public void setSelection(boolean selected) {
		this.selected = selected;
		redraw();
	}
	
	/**
	 * Returns the selection.
	 * 
	 * @return <code>true</code> if selected
	 */
	public boolean getSelection() {
		return selected;
	}

	/**
	 * Adds a new listener to selection events.
	 * If the listener is already added, this method does nothing.
	 * 
	 * @param listener Listener to add
	 */
	public void addSelectionListener(SelectionListener listener) {
		if (selectionListeners == null)
			selectionListeners = new ListenerList();
		selectionListeners.add(listener);
	}
	
	/**
	 * Removes a listener from selection events.
	 * 
	 * @param listener Listener to remove
	 */
	public void removeSelectionListener(SelectionListener listener) {
		if (selectionListeners != null)
			selectionListeners.remove(listener);
	}
	
	/**
	 * Selects the button if it is enabled and does not have the SWT.READ_ONLY
	 * flag set.
	 * If the button has SWT.TOGGLE flag set, it's selection will be toggled.
	 * If the button has SWT.RADIO flag set, it will be selected and all other
	 * radio buttons in the parent will be unselected.
	 * 
	 * @param down <code>true</code> if mouse or key is currently down
	 */
	protected void select(boolean down) {
		if (canUpdate()) {
			// Toggle
			if ((getStyle() & SWT.TOGGLE) == SWT.TOGGLE) {
				if (down) {
					setSelection(!getSelection());
					notifySelectionListeners();
				}
			}
			// Radio
			else if ((getStyle() & SWT.RADIO) == SWT.RADIO) {
				if (down && !getSelection()) {
					setSelection(true);
					Control[] siblings = getParent().getChildren();
					for (Control sibling : siblings) {
						if ((sibling instanceof InfoButton) && !sibling.equals(this)) {
							InfoButton otherButton = (InfoButton)sibling;
							if ((otherButton.getStyle() & SWT.RADIO) == SWT.RADIO) {
								otherButton.setSelection(false);
							}
						}
					}
					notifySelectionListeners();
				}
			}
			// Normal
			else {
				setSelection(down);
				if (down)
					notifySelectionListeners();
			}
		}
	}
	
	/**
	 * Notifies selection listeners.
	 */
	private void notifySelectionListeners() {
		if (selectionListeners != null) {
			Event event = new Event();
			event.widget = this;
			SelectionEvent selectionEvent = new SelectionEvent(event);
			Object[] listeners = selectionListeners.getListeners();
			for (Object listener : listeners) {
				try {
					((SelectionListener)listener).widgetSelected(selectionEvent);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Computes the size of the label.
	 * 
	 * 		                            <vertical margin>
	 * --------------------------------------------------------------------------------------
	 * |<horizontal margin> | <image> | <text margin> | <text>        | <horizontal margin> |
	 * |                    |         |               | <description> |                     |
	 * --------------------------------------------------------------------------------------
	 * 		                            <vertical margin>
	 */
	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		GC gc = new GC(this);

		// Computed width of label
		int width = getHorizontalMargin();
		// Computed height of label
		int height = 0;
		// Image size
		if (getImage() != null) {
			// Width of image plus margin between image and text
			width += getImage().getImageData().width + getTextMargin();
			// Height of image
			height = getImage().getImageData().height;
		}
		
		// Text size
		Point textSize = new Point(0, 0);
		if (getText() != null) {
			gc.setFont(getLabelFont());
			textSize = gc.textExtent(getText());
		}
		// Description size
		if (getDescription() != null) {
			gc.setFont(getDescriptionFont());
			textLayout.setFont(getDescriptionFont());
			Point descriptionSize = gc.textExtent(getDescription().replace('\n', ' '));
			textLayout.setText(getDescription());
			int descriptionWidth;
			// If default width then use extents of description
			if (wHint == SWT.DEFAULT){
				descriptionWidth = descriptionSize.x;
			}
			// Else use remaining width
			else {
				descriptionWidth = wHint - width;
				if (descriptionWidth <= 0)
					descriptionWidth = 100;
			}
			// If wrapped description with is greater than the text width then
			// use it for the total text width
			if (descriptionWidth > textSize.x)
				textSize.x = descriptionWidth;
			textLayout.setWidth(descriptionWidth);
			// Add description height
			textSize.y += textLayout.getLineCount() * descriptionSize.y;
		}
		// Include total text width
		width += textSize.x;
		// If text size is greater than image size then use it for height
		if (textSize.y > height)
			height = textSize.y;
		
		// Include vertical margins in height
		height += getVerticalMargin() + getVerticalMargin();
		// Include right horizontal margin in width
		width += getHorizontalMargin();

		gc.dispose();
		
		Point size = new Point(width, (hHint == SWT.DEFAULT) ? height : hHint);
		
		return size;
	}
	
	/**
	 * Called to paint the label.
	 * 
	 * @param event Paint event
	 */
	protected void onPaint(PaintEvent event) {
		Rectangle clientArea = getClientArea();
		GC gc = event.gc;
		
		boolean selectForeground = tracking || getSelection();
		
		// Default background
		gc.setBackground(getBackground());
		if (canUpdate()) {
			// Selected background
			if (getSelection()) {
				gc.setBackground(getColor(ElementColor.selectBackground));
			}
			// Hover background
			else if (tracking) {
				gc.setBackground(getColor(ElementColor.hoverBackground));
			}
		}
		// Paint background
		if (getRounded())
			gc.fillRoundRectangle(clientArea.x, clientArea.y, clientArea.width, clientArea.height, ROUND_SIZE, ROUND_SIZE);
		else
			gc.fillRectangle(clientArea);

		// Compute label size
		Point size = computeSize(clientArea.width, SWT.DEFAULT);
		
		int yOffset = getVerticalMargin();
		int xOffset = getHorizontalMargin();
		// Draw label image
		Image itemImage = getImage();
		if (itemImage != null) {
			if (!isEnabled()) {
				itemImage = disabledImage;
			}
			gc.drawImage(itemImage, xOffset, yOffset + (size.y - getVerticalMargin()) / 2 - itemImage.getImageData().height / 2);
			xOffset = itemImage.getImageData().width + getTextMargin();
		}

		// Compute text height
		int textHeight = 0;
		if (getText() != null) {
			gc.setFont(getLabelFont());
			textHeight = gc.textExtent(getText()).y;
		}

		// Compute description height
		int descriptionHeight = 0;
		if (getDescription() != null) {
			gc.setFont(getDescriptionFont());
			textLayout.setFont(getDescriptionFont());
			textLayout.setText(getDescription());
			textLayout.setWidth(clientArea.width - xOffset);
			Point descriptionSize = gc.textExtent(getDescription().replace('\n', ' '));
			descriptionHeight = textLayout.getLineCount() * descriptionSize.y;
		}
		
		// Compute centered text vertical offset
		int yTextOffset = yOffset + size.y  / 2 - textHeight / 2 - descriptionHeight / 2 - getVerticalMargin();
		
		// Draw label text
		if (getText() != null) {
			gc.setFont(getLabelFont());
			gc.setForeground(selectForeground ? getColor(ElementColor.selectedLabel) : getColor(ElementColor.label));
			
			String labelText = getText();
			if (getShortenText()) {
				Point sz = gc.textExtent(labelText);
				if (sz.x > clientArea.width - xOffset)
					labelText = shortenText(gc, labelText, clientArea.width - xOffset);
			}
			
			gc.drawText(labelText, xOffset, yTextOffset, DRAW_FLAGS);
			yTextOffset += textHeight;
		}
		// Draw label description
		if (getDescription() != null) {
			gc.setForeground(selectForeground ? getColor(ElementColor.selectedDescription) : getColor(ElementColor.description));
			textLayout.draw(gc, xOffset, yTextOffset);
		}
		
		// Draw focus
		if (hasFocus && canUpdate()) {
			gc.setLineDash(FOCUS_LINE_STYLE);
			gc.setForeground(getSelection() ? getColor(ElementColor.selectedLabel) : getColor(ElementColor.label));
			if (getRounded())
				gc.drawRectangle(clientArea.x, clientArea.y, clientArea.width - 1, clientArea.height - 1);
			else
				gc.drawRoundRectangle(clientArea.x, clientArea.y, clientArea.width - 1, clientArea.height - 1, ROUND_SIZE, ROUND_SIZE);
		}
	}

	/**
	 * Shorten the given text <code>t</code> so that its length doesn't exceed
	 * the given width. The default implementation replaces characters in the
	 * center of the original string with an ellipsis ("...").
	 * Override if you need a different strategy.
	 * 
	 * @param gc the gc to use for text measurement
	 * @param t the text to shorten
	 * @param width the width to shorten the text to, in pixels
	 * @return the shortened text
	 * Note, this code was copied from 
	 *   org.eclipse.swt.custom.CLabel.shortenText()
	 */
	protected String shortenText(GC gc, String t, int width) {
		if (t == null) return null;
		int w = gc.textExtent(ELLIPSIS, DRAW_FLAGS).x;
		if (width<=w) return t;
		int l = t.length();
		int max = l/2;
		int min = 0;
		int mid = (max+min)/2 - 1;
		if (mid <= 0) return t;
		TextLayout layout = new TextLayout (getDisplay());
		layout.setText(t);
		mid = validateOffset(layout, mid);
		while (min < mid && mid < max) {
			String s1 = t.substring(0, mid);
			String s2 = t.substring(validateOffset(layout, l-mid), l);
			int l1 = gc.textExtent(s1, DRAW_FLAGS).x;
			int l2 = gc.textExtent(s2, DRAW_FLAGS).x;
			if (l1+w+l2 > width) {
				max = mid;			
				mid = validateOffset(layout, (max+min)/2);
			} else if (l1+w+l2 < width) {
				min = mid;
				mid = validateOffset(layout, (max+min)/2);
			} else {
				min = max;
			}
		}
		String result = mid == 0 ? t : t.substring(0, mid) + ELLIPSIS + t.substring(validateOffset(layout, l-mid), l);
		layout.dispose();
	 	return result;
	}
	
	/**
	 * Validates text layout offset.
	 * 
	 * @param layout Layout
	 * @param offset Offset
	 * @return Next offset if it fits, previous offset otherwise
	 * 
	 * Note, this code was copied from
	 *   org.eclipse.swt.custom.CLabel.validateOffset()
	 */
	int validateOffset(TextLayout layout, int offset) {
		int nextOffset = layout.getNextOffset(offset, SWT.MOVEMENT_CLUSTER);
		if (nextOffset != offset) return layout.getPreviousOffset(nextOffset, SWT.MOVEMENT_CLUSTER);
		return offset;
	}
}
