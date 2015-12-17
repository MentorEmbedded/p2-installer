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

import java.util.ArrayList;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.codesourcery.installer.ui.InfoButton.ElementColor;

/**
 * Displays a scrollable list of 
 * {@link com.codesourcery.installer.ui.InfoButton}.
 */
public class InfoList extends ScrolledComposite {
	/** List items */
	private ArrayList<InfoButton> items = new ArrayList<InfoButton>();
	/** Label font */
	private Font labelFont;
	/** Hover color */
	private Color hoverColor;
	/** Items area */
	private Composite itemsArea;
	/** Selection listeners */
	private ListenerList selectionListeners;
	/** Mouse listeners */
	private ListenerList mouseListeners;
	/** <code>true</code> if text should be shortened */
	private boolean shortenText = false;

	/**
	 * Constructor
	 * 
	 * @param parent Parent
	 * @param style Styles
	 */
	public InfoList(Composite parent, int style) {
		super(parent, style);
		
		// Create hover color
		hoverColor = new Color(getShell().getDisplay(), 
				blendRGB(
						getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB(), 
						getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB(), 
						40));
		
		// Create options area
		itemsArea = new Composite(this, SWT.NONE);
		GridLayout itemsLayout = new GridLayout(1, true);
		itemsLayout.verticalSpacing = 0;
		itemsArea.setLayout(itemsLayout);
		
		setExpandHorizontal(true);
		setExpandVertical(true);

		// Handle items resize
		itemsArea.addControlListener(new ControlAdapter() {
			int width = -1;
			@Override
			public void controlResized(ControlEvent e) {
				getShell().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						int newWidth = itemsArea.getSize().x;
						if (newWidth != width) {
							width = newWidth;
							int minHeight = itemsArea.computeSize(newWidth, SWT.DEFAULT).y;
							setMinHeight(minHeight);
						}
					}
				});
			}
		});
		
		setContent(itemsArea);
		setShowFocusedControl(true);
	}
	
	@Override
	public void dispose() {
		if (hoverColor != null) {
			hoverColor.dispose();
			hoverColor = null;
		}
		
		super.dispose();
	}
	
	/**
	 * Returns the area for items.
	 * 
	 * @return Items area
	 */
	protected Composite getItemsArea() {
		return itemsArea;
	}

	/**
	 * Blends two RGB values using the provided ratio. 
	 * 
	 * @param c1 First RGB value
	 * @param c2 Second RGB value
	 * @param ratio Percentage of the first RGB to blend with 
	 * second RGB (0-100)
	 * 
	 * @return The RGB value of the blended color
	 */
	public static RGB blendRGB(RGB c1, RGB c2, int ratio) {
		ratio = Math.max(0, Math.min(255, ratio));

		int r = Math.max(0, Math.min(255, (ratio * c1.red + (100 - ratio) * c2.red) / 100));
		int g = Math.max(0, Math.min(255, (ratio * c1.green + (100 - ratio) * c2.green) / 100));
		int b = Math.max(0, Math.min(255, (ratio * c1.blue + (100 - ratio) * c2.blue) / 100));
		
		return new RGB(r, g, b);
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
	 * Sets the label font.
	 * 
	 * @param labelFont Label font
	 */
	public void setLabelFont(Font labelFont) {
		this.labelFont = labelFont;
		
		for (InfoButton item : items) {
			item.setLabelFont(labelFont);
		}
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
	 * Adds a new item.
	 * 
	 * @param image Item image or <code>null</code>
	 * @param text Item text
	 * @param description Item description or <code>null</code>
	 * @return New item
	 */
	public InfoButton addItem(Image image, String text, String description) {
		InfoButton button = new InfoButton(getItemsArea(), SWT.RADIO);
		button.setShortenText(getShortenText());
		
		button.setImage(image);
		button.setText(text);
		button.setDescription(description);
		button.setRounded(true);
		button.setColor(ElementColor.description, getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		button.setLayoutData(gridData);
		button.setColor(ElementColor.hoverBackground, hoverColor);
		button.setLabelFont(getLabelFont());
		// Selection listener
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				notifySelectionListeners(e);
			}
		});
		// Mouse listener
		button.addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				notifyMouseListeners(3, e);
			}

			@Override
			public void mouseDown(MouseEvent e) {
				notifyMouseListeners(1, e);
			}

			@Override
			public void mouseUp(MouseEvent e) {
				notifyMouseListeners(2, e);
			}
		});
		
		return button;
	}

	/**
	 * Adds a new item.
	 * 
	 * @param text Item text
	 * @return New item
	 */
	public InfoButton addItem(String text) {
		InfoButton item = addItem(null, text, null);
		items.add(item);
		
		return item;
	}

	/**
	 * Removes an item.
	 * 
	 * @param item Item to remove
	 */
	public void removeItem(InfoButton item) {
		items.remove(item);
		item.dispose();
		getItemsArea().layout(true);
	}
	
	/**
	 * Removes all items.
	 */
	public void removeAllItems() {
		for (InfoButton item : items) {
			item.dispose();
		}
		getItemsArea().layout(true);
	}

	/**
	 * Returns the items.
	 * 
	 * @return Items
	 */
	public InfoButton[] getItems() {
		return items.toArray(new InfoButton[items.size()]);
	}

	/**
	 * Notifies selection listeners.
	 * 
	 * @param event Selection event
	 */
	protected void notifySelectionListeners(SelectionEvent event) {
		Object[] listeners = selectionListeners.getListeners();
		for (Object listener : listeners) {
			((SelectionListener)listener).widgetSelected(event);
		}
	}

	/**
	 * Notifies mouse listeners.
	 * 
	 * @param type Event type
	 * @param event Mouse event
	 */
	protected void notifyMouseListeners(int type, MouseEvent event) {
		Object[] listeners = mouseListeners.getListeners();
		for (Object listener : listeners) {
			if (type == 1) {
				((MouseListener)listener).mouseDown(event);
			}
			else if (type == 2) {
				((MouseListener)listener).mouseUp(event);
			}
			else if (type == 3) {
				((MouseListener)listener).mouseDoubleClick(event);
			}
		}
	}

	/**
	 * Adds a listener to selection events.  If the listener has already been 
	 * added, this method does nothing.
	 * 
	 * @param listener Listener to add
	 */
	public void addSelectionListener(SelectionListener listener) {
		if (selectionListeners == null) {
			selectionListeners = new ListenerList();
		}
		
		selectionListeners.add(listener);
	}
	
	/**
	 * Removes a listener from selection events.
	 * 
	 * @param listener Listener to remove
	 */
	public void removeSelectionListener(SelectionListener listener) {
		if (selectionListeners != null) {
			selectionListeners.remove(listener);
		}
	}

	/**
	 * Adds a listener to mouse events.  If the listener has already been
	 * added, this method does nothing.
	 * 
	 * @Param listener Listener to add
	 */
	public void addMouseListener(MouseListener listener) {
		if (mouseListeners == null) {
			mouseListeners = new ListenerList();
		}
		
		mouseListeners.add(listener);
	}
	
	/**
	 * Removes a listener from mouse events.
	 * 
	 * @Param listener Listener to remove
	 */
	public void removeMouseListener(MouseListener listener) {
		if (mouseListeners != null) {
			mouseListeners.remove(listener);
		}
	}
}
