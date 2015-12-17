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
package com.codesourcery.internal.installer.ui;

import java.util.ArrayList;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;

import com.codesourcery.internal.installer.ui.DetailTreeItem.ItemRegion;

/**
 * Tree control that supports check-boxes and detail text.
 */
public class DetailTree extends  Canvas implements ISelectionProvider, ICheckable {
	/** Tree image types */
	public enum ImageType {
		/** Collapsed item image */
		COLLAPSED,
		/** Expanded item image */
		EXPANDED,
		/** Checked item image */
		CHECKED,
		/** Un-checked item image */
		UNCHECKED,
		/** No check item image */
		NOCHECK
	}

	/** No items constant */
	private static final DetailTreeItem[] NO_ITEMS = new DetailTreeItem[0];
	/** Text drawing flags */
	private final static int TEXT_FLAGS = SWT.DRAW_TRANSPARENT | SWT.DRAW_MNEMONIC;
	/** Margin between image */
	private final static int ITEM_IMAGE_MARGIN = 4;
	/** Vertical margin between icons */
	private final static int ITEM_VERTICAL_MARGIN = 6;
	/** Description vertical margin */
	private final static int DESCRIPTION_VERTICAL_MARGIN = 2;
	/** Tree images */
	private Image[] images = new Image[ImageType.values().length];
	/** Root items */
	DetailTreeItem[] rootItems = NO_ITEMS;
	/** Text layout */
	private TextLayout textLayout;
	/** Description foreground color */
	private Color descriptionForeground;
	/** Hover background color */
	private Color hoverBackground;
	/** Selected item or <code>null</code> */
	private DetailTreeItem selectedItem;
	/** Hovered item or <code>null</code> */
	private DetailTreeItem hoverItem;
	/** Description font */
	private Font descriptionFont;
	/** Default computed size */
	private Point defaultSize;
	/** Current scroll offset */
	private Point scrollOffset = new Point(0, 0);
	/** Vertical scroll increment */
	private int verticalScrollIncrement = 16;
	/** <code>true</code> if control has focus */
	private boolean hasFocus;
	/** <code>true</code> if description text should be wrapped */
	private boolean wrapDescription;
	/** Selection listeners */
	private ListenerList selectionListeners = new ListenerList();
	/** Check listeners */
	private ListenerList checkListeners = new ListenerList();
	/** <code>true</code> to draw horizontal separator between items */
	private boolean drawSeparator = false;

	/**
	 * Constructor
	 * 
	 * @param parent Parent
	 * @param style Style flags can include:
	 * <ul>
	 * <li>SWT.V_SCROLL - Enable vertical scrolling</li>
	 * <li>SWT.H_SCROLL - Enable horizontal scrolling</li>
	 * <li>SWT.NO_FOCUS<li> - Don't show focus</li>
	 * <li>SWT.WRAP - Wrap description text</li>
	 * <li>DOUBLE_BUFFERED - Double-buffer drawing to reduce flicker</li>
	 * </ul>
	 */
	public DetailTree(Composite parent, int style) {
		super(parent, style | SWT.NO_BACKGROUND);

		wrapDescription = ((style & SWT.WRAP) == SWT.WRAP);
		
		// Create text layout
		textLayout = new TextLayout(getShell().getDisplay());

		// Description foreground color
		descriptionForeground = new Color(getShell().getDisplay(), 
				blendRGB(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND).getRGB(), 
						getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB(), 
						40));
		
		// Hover background color
		hoverBackground = new Color(getShell().getDisplay(), 
				blendRGB(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB(), 
						getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB(), 
						45));

		// Paint listener
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				onPaint(e.gc);
			}
		});
		// Handle mouse buttons
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				DetailTreeItem item = hitTest(e.x, e.y, ItemRegion.TEXT);
				// If label double-clicked, toggle expansion
				if (item != null) {
					toggleExpand(item);
				}
			}

			@Override
			public void mouseDown(MouseEvent e) {
				onMouseDown(e.x, e.y);
			}
		});
		// Handle mouse tracking
		addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(MouseEvent e) {
				if (hoverItem != null) {
					DetailTreeItem previousHoverItem = hoverItem;
					hoverItem = null;
					redraw(previousHoverItem, ItemRegion.TEXT);
				}
			}
		});
		// Handle mouse moves
		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				DetailTreeItem item = hitTest(e.x, e.y, ItemRegion.TEXT);
				if (item != hoverItem) {
					DetailTreeItem previousItem = hoverItem;
					// Set new hover item unless it is selected
					hoverItem = (item == getSelectedItem()) ? null : item;
					// Updated previous hover item if any
					redraw(previousItem, ItemRegion.TEXT);
					// Update new hover item
					redraw(hoverItem, ItemRegion.TEXT);
				}
			}
		});
		// Handle control resize
		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				recalcScrollBars();
				
				// Reset scroll offset to show more control when sized taller
				ScrollBar vScrollBar = getVerticalBar();
				if (vScrollBar != null) {
					scrollOffset.y = vScrollBar.getSelection();
					redraw();
				}
			}
		});

		// Initialize scroll bars
		recalcScrollBars();
		// Handle horizontal scrolling
		final ScrollBar hScrollBar = getHorizontalBar();
		if (hScrollBar != null) {
			hScrollBar.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					scrollOffset.x = hScrollBar.getSelection();
					redraw();
				}
			});
		}
		// Handle vertical scrolling
		final ScrollBar vScrollBar = getVerticalBar();
		if (vScrollBar != null) {
			vScrollBar.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					scrollOffset.y = vScrollBar.getSelection();
					redraw();
				}
			});
		}
		// Listen to focus
		addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
					if (isEnabled()) {
					if ((getStyle() & SWT.NO_FOCUS) != SWT.NO_FOCUS) {
						hasFocus = true;
						redraw(getSelectedItem(), ItemRegion.TEXT);
					}
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (isEnabled()) {
					if ((getStyle() & SWT.NO_FOCUS) != SWT.NO_FOCUS) {
						hasFocus = false;
						redraw(getSelectedItem(), ItemRegion.TEXT);
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
		// Handle key pressed
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (isEnabled()) {
					// Arrow down
					if (e.keyCode == SWT.ARROW_DOWN) {
						select(true);
					}
					// Arrow up
					else if (e.keyCode == SWT.ARROW_UP) {
						select(false);
					}
					
					DetailTreeItem item = getSelectedItem();
					if (item != null) {
						// Arrow left
						if (e.keyCode == SWT.ARROW_LEFT) {
							item.collapse();
						}
						// Arrow right
						else if (e.keyCode == SWT.ARROW_RIGHT) {
							item.expand();
						}
						// Space - select item
						else if (e.keyCode == SWT.SPACE) {
							if ((item.getStyle() & SWT.CHECK) == SWT.CHECK) {
								item.setChecked(!item.isChecked(), true);
							}
						}
					}
				}
				e.doit = true;
			}
		});
		
		// Set font
		setFont(parent.getFont());
	}

	@Override
	public void dispose() {
		if (descriptionForeground != null) {
			descriptionForeground.dispose();
			descriptionForeground = null;
		}
		if (hoverBackground != null) {
			hoverBackground.dispose();
			hoverBackground = null;
		}
		super.dispose();
	}
	
	/**
	 * Selects the next or previous item.  The item is revealed if necessary.
	 * 
	 * @param next <code>true</code> to select the next item, <code>false</code> 
	 * to select the previous item.
	 */
	private void select(final boolean next) {
		visitItems(new ItemVisitor() {
			DetailTreeItem lastItem = null;
			
			@Override
			public boolean visit(DetailTreeItem item) {
				if (!item.isRevealed())
					return true;
				// Select next item
				if (next) {
					if (lastItem != null) {
						setSelectedItem(item);
						reveal(item);
						notifySelectionChanged();
						return false;
					}
					else if (getSelectedItem() == item) {
						lastItem = item;
					}
				}
				// Select previous item
				else {
					if (getSelectedItem() == item) {
						if (lastItem != null) {
							setSelectedItem(lastItem);
							reveal(lastItem);
						}
						return false;
					}
					else {
						lastItem = item;
					}
				}

				return true;
			}
		});
	}

	/**
	 * Redraws a region of a single item.  This is used instead of 
	 * <code>redraw()</code> when only an item changes to avoid flicker.
	 * 
	 * @param item Item to redraw
	 * @param region Region to redraw
	 */
	void redraw(DetailTreeItem item, ItemRegion region) {
		if (item != null) {
			Rectangle itemArea = item.getRegion(region);
			if (itemArea != null) {
				redraw(itemArea.x, itemArea.y, itemArea.width, itemArea.height, true);
			}
		}
	}

	/**
	 * Expands all items.
	 */
	public void expandAll() {
		for (DetailTreeItem rootItem : getRootItems()) {
			rootItem.expandAll();
		}
	}
	
	/**
	 * Reveals an item, scrolling vertically if necessary.
	 * 
	 * @param item Item to reveal
	 */
	public void reveal(DetailTreeItem item) {
		// Clear hover item
		hoverItem = null;

		// If item is not revealed
		if (!item.isRevealed()) {
			// Expand all parent items
			DetailTreeItem parent = item.getParent();
			while (parent != null) {
				parent.expand();
				parent = parent.getParent();
			}
		}
		
		ScrollBar vScrollBar = getVerticalBar();
		if (vScrollBar != null) {
			Rectangle clientArea = getClientArea();
			int itemExtent = item.getArea().y + item.getArea().height;

			// Bottom of item is below visible area
			if (itemExtent > clientArea.height) {
				scrollOffset.y += itemExtent - clientArea.height;
				vScrollBar.setSelection(scrollOffset.y);
				redraw();
			}
			// Top of item is above visible area
			else if (item.getArea().y < 0) {
				scrollOffset.y += item.getArea().y;
				vScrollBar.setSelection(scrollOffset.y);
				redraw();
			}
		}
	}
	
	/**
	 * Calculates the extends and visibility of horizontal and vertical
	 * scroll bars.
	 */
	void recalcScrollBars() {
		ScrollBar hScrollBar = getHorizontalBar();
		ScrollBar vScrollBar = getVerticalBar();
		
		// If either horizontal or vertical scrolling is enabled
		if ((hScrollBar != null) || (vScrollBar != null)) {
			// Compute default size of control
			Point size = computeSize(SWT.DEFAULT, SWT.DEFAULT);
			// Get the visible client area
			Rectangle clientArea = getClientArea();

			if (hScrollBar != null) {
				// Show horizontal scroll bar if content does not fit horizontally
				hScrollBar.setVisible(size.x > clientArea.width);
				
				// Set the maximum horizontal scroll to be the default width of 
				// control minus what will show in the client area. 
				hScrollBar.setMaximum(size.x);
				hScrollBar.setThumb(clientArea.width);
				hScrollBar.setPageIncrement(clientArea.width);
			}
			if (vScrollBar != null) {
				// Show vertical scroll bar if content does not fit vertically
				vScrollBar.setVisible(size.y > clientArea.height);
				// Set the maximum vertical scroll to be the default height of 
				// control minus what will show in the client area. 
				vScrollBar.setMaximum(size.y);
				vScrollBar.setIncrement(verticalScrollIncrement);
				vScrollBar.setThumb(clientArea.height);
				vScrollBar.setPageIncrement(clientArea.height);
			}
		}
	}
	
	/**
	 * Returns the vertical scroll bar width.
	 * 
	 * @return Width
	 */
	protected int getVerticalScrollBarWidth() {
		int width = 0;
		ScrollBar scrollBar = getVerticalBar();
		if (scrollBar != null) {
			width = scrollBar.getSize().x;
		}
		return width;
	}
	
	/**
	 * Returns the horizontal scroll bar height.
	 * 
	 * @return Height
	 */
	protected int getHorizontalScrollBarHeight() {
		int height = 0;
		ScrollBar scrollBar = getHorizontalBar();
		if (scrollBar != null) {
			height = scrollBar.getSize().y;
		}
		return height;
	}

	/**
	 * Checks if coordinates correspond to an item.
	 * 
	 * @param x Horizontal coordinate
	 * @param y Vertical coordinate
	 * @param region Item region to test for or <code>null</code> for the
	 * entire item region
	 * @return Item or <code>null</code>
	 */
	public DetailTreeItem hitTest(final int x, final int y, final ItemRegion region) {
		final DetailTreeItem[] hitItem = new DetailTreeItem[] { null };
		
		visitItems(new ItemVisitor() {
			@Override
			public boolean visit(DetailTreeItem item) {
				// Entire item area 
				if (region == null) {
					Rectangle itemArea = item.getArea();
					if ((itemArea != null) && itemArea.contains(x, y)) {
						hitItem[0] = item;
						return false;
					}
				}
				// Item region
				else {
					ItemRegion itemRegion = item.hitTest(x, y);
					if (itemRegion == region) {
						hitItem[0] = item;
						return false;
					}
				}
				return true;
			}
		});
		
		return hitItem[0];
	}

	/**
	 * Adds a new item.
	 * 
	 * @param item Item
	 */
	void addItem(DetailTreeItem item) {
		DetailTreeItem[] newItems = new DetailTreeItem[rootItems.length + 1];
		System.arraycopy(rootItems, 0, newItems, 0, rootItems.length);
		newItems[newItems.length - 1] = item;
		rootItems = newItems;
		recalcScrollBars();
		redraw();
	}

	/**
	 * Sets the selected item.
	 * 
	 * @param selectedItem Item or <code>null</code> to clear selection
	 */
	private void setSelectedItem(DetailTreeItem selectedItem) {
		DetailTreeItem previousItem =  this.selectedItem;
		this.selectedItem = selectedItem;
		redraw(previousItem, ItemRegion.TEXT);
		redraw(selectedItem, ItemRegion.TEXT);
	}

	/**
	 * Returns the selected item.
	 * 
	 * @return Selected item or <code>null</code>
	 */
	private DetailTreeItem getSelectedItem() {
		return selectedItem;
	}

	/**
	 * Sets all items checked or un-checked.
	 * 
	 * @param checked <code>true</code> to set checked, <code>true</code> to
	 * set un-checked
	 */
	public void setAllChecked(final boolean checked) {
		visitItems(new ItemVisitor() {
			@Override
			public boolean visit(DetailTreeItem item) {
				if ((item.getStyle() & SWT.CHECK) == SWT.CHECK) {
					item.setChecked(checked);
				}
				return true;
			}
		});
	}
	
	/**
	 * Removes all items from the tree.
	 */
	public void removeAll() {
		rootItems = NO_ITEMS;
		scrollOffset = new Point(0, 0);
		recalcScrollBars();
		redraw();
	}
	
	/**
	 * Returns root items.
	 * 
	 * @return Root times
	 */
	public DetailTreeItem[] getRootItems() {
		return rootItems;
	}
	
	/**
	 * Returns all items.
	 * 
	 * @return All items
	 */
	public DetailTreeItem[] getAllItems() {
		final ArrayList<DetailTreeItem> items = new ArrayList<DetailTreeItem>();
		visitItems(new ItemVisitor() {
			@Override
			public boolean visit(DetailTreeItem item) {
				items.add(item);
				return true;
			}
		});
		
		return items.toArray(new DetailTreeItem[items.size()]);
	}

	/**
	 * Returns items that are checked.
	 * 
	 * @return Checked items
	 */
	public DetailTreeItem[] getCheckedItems() {
		final ArrayList<DetailTreeItem> items = new ArrayList<DetailTreeItem>();
		visitItems(new ItemVisitor() {
			@Override
			public boolean visit(DetailTreeItem item) {
				if (item.isChecked()) {
					items.add(item);
				}
				return true;
			}
		});
		
		return items.toArray(new DetailTreeItem[items.size()]);
	}

	/**
	 * Sets a tree image.
	 * 
	 * @param type Image type
	 * @param image Image or <code>null</code>
	 */
	public void setImage(ImageType type, Image image) {
		images[type.ordinal()] = image;
	}

	/**
	 * Returns a tree image.
	 * 
	 * @param type Image type
	 * @return Image or <code>null</code>
	 */
	public Image getImage(ImageType type) {
		return images[type.ordinal()];
	}

	/**
	 * Sets the description font.
	 * 
	 * @param descriptionFont Font
	 */
	public void setDescriptionFont(Font descriptionFont) {
		this.descriptionFont = descriptionFont;
	}
	
	/**
	 * Returns the description font.
	 * 
	 * @return Font
	 */
	public Font getDescriptionFont() {
		if (descriptionFont == null)
			return getFont();
		else
			return descriptionFont;
	}
	
	/**
	 * Sets if a horizontal separator will be drawn between root items.
	 * 
	 * @param drawSeparator <code>true</code> to draw separator
	 */
	public void setDrawSeparator(boolean drawSeparator) {
		this.drawSeparator = drawSeparator;
	}
	
	/**
	 * Returns if a horizontal separator will be drawn between root items.
	 * 
	 * @return <code>true</code> if separator will be drawn
	 */
	public boolean getDrawSeparator() {
		return drawSeparator;
	}
	
	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		Point offset = new Point(0, 0);
		defaultSize = new Point(0, 0);

		// Compute the preferred size
		GC gc = new GC(this);
		for (DetailTreeItem item : getRootItems()) {
			offset = paintItem(gc, offset, item, false);
		}
		gc.dispose();

		Point computedSize = super.computeSize(wHint, hHint, changed);
		if (wHint == SWT.DEFAULT)
			computedSize.x = defaultSize.x;
		if (hHint == SWT.DEFAULT)
			computedSize.y = defaultSize.y;
		
		return computedSize;
	}

	/**
	 * Returns the offset of a value centered in another value.
	 * 
	 * @param size Value to center in
	 * @param value Value to center
	 * @return Offset of value for centered alignment
	 */
	private int center(int size, int value) {
		if (size == value) {
			return 0;
		}
		else {
			return size / 2 - value / 2;
		}
	}

	/**
	 * Paints an item or computes it size.
	 * 
	 * @param gc Graphics context
	 * @param offset Horizontal offset of item
	 * @param item Item
	 * @param draw <code>true</code> to draw item or <code>false</code> to only
	 * compute the default size.
	 * 
	 * @return Offset of next item
	 */
	private Point paintItem(GC gc, Point offset, DetailTreeItem item, boolean draw) {
		// Initialize item area origin (width and height computed later)
		Rectangle itemArea = new Rectangle(offset.x, offset.y, 0, 0);
		
		// Clear item hit regions
		if (draw) {
			item.clearRegions();
		}

		// Expand/collapse image
		Image expandImage = item.isExpanded() ? 
				getImage(ImageType.EXPANDED) : 
				getImage(ImageType.COLLAPSED);
		ImageData expandImageData = expandImage.getImageData();
		
		// Check-box image
		Image checkImage;
		if ((item.getStyle() & SWT.CHECK) == SWT.CHECK) {
			checkImage = item.isChecked() ?
					getImage(ImageType.CHECKED) :
					getImage(ImageType.UNCHECKED);
		}
		else {
			checkImage = getImage(ImageType.NOCHECK);
		}
		ImageData checkImageData = checkImage.getImageData();

		// Set label font
		gc.setFont(getFont());
		// Compute size of label text
		Point textSize = (item.getText() != null) ? gc.textExtent(item.getText(), TEXT_FLAGS) : new Point(0, 0);
		
		// The label height will be the height of the largest image or label text
		int labelHeight = Math.max(Math.max(textSize.y, expandImageData.height), checkImageData.height);
		
		// Region drawing offset
		int xOffset = offset.x;
		
		// If item has children, draw expand image
		if (item.hasChildren()) {
			Rectangle expandRegion = new Rectangle(xOffset, offset.y + 
					center(labelHeight, expandImageData.height) + 2, expandImageData.width + ITEM_IMAGE_MARGIN, expandImageData.height);
			if (draw) {
				gc.drawImage(expandImage, expandRegion.x, expandRegion.y);
				item.setRegion(ItemRegion.EXPAND, expandRegion);
			}
		}
		xOffset += expandImageData.width + ITEM_IMAGE_MARGIN;
		
		// Draw check-box image
		Rectangle checkBoxRegion = new Rectangle(xOffset, offset.y + 
				center(labelHeight, checkImageData.height) + 2, checkImageData.width + ITEM_IMAGE_MARGIN, checkImageData.height);
		if (draw) {
			gc.drawImage(checkImage, checkBoxRegion.x, checkBoxRegion.y);
			item.setRegion(ItemRegion.CHECKBOX, checkBoxRegion);
		}
		xOffset += checkImageData.width + ITEM_IMAGE_MARGIN;

		// Draw label
		int labelVerticalOffset = offset.y + center(labelHeight, textSize.y);
		if (draw) {
			// Selected
			if (item == getSelectedItem()) {
				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
				gc.setBackground(hasFocus ? gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION) : hoverBackground);
			}
			// Normal
			else {
				gc.setForeground((hoverItem == item) ? 
					gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT) :
					gc.getDevice().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
				gc.setBackground((hoverItem == item) ? 
						hoverBackground :
						gc.getDevice().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			}
			gc.fillRoundRectangle(xOffset, labelVerticalOffset, textSize.x, textSize.y, 2, 2);
			gc.drawText(item.getText(), xOffset, labelVerticalOffset, TEXT_FLAGS);
			item.setRegion(ItemRegion.TEXT, new Rectangle(xOffset, labelVerticalOffset, textSize.x, textSize.y));
		}

		// Update default width
		if (xOffset + textSize.x > defaultSize.x) {
			defaultSize.x = xOffset + textSize.x;
		}
		
		// Set description offset past text
		int descOffset = offset.y + textSize.y + DESCRIPTION_VERTICAL_MARGIN;
		// Update offset past text area
		offset.y += labelHeight;
		
		// Draw description if available
		if (item.getDescription() != null) {
			gc.setForeground(descriptionForeground);
			textLayout.setFont(getDescriptionFont());

			if (wrapDescription) {
				Rectangle clientArea = getClientArea();
				int width = clientArea.width - xOffset;
				if (width < 0)
					width = 1;
				textLayout.setWidth(width);
			}

			textLayout.setText(item.getDescription());
			Rectangle descriptionBounds = textLayout.getBounds();
			if (xOffset + descriptionBounds.width > defaultSize.x) {
				defaultSize.x = xOffset + descriptionBounds.width - getVerticalScrollBarWidth();
			}
			if (draw) {
				textLayout.draw(gc, xOffset, descOffset);
				item.setRegion(ItemRegion.DESCRIPTION, new Rectangle(xOffset, descOffset, descriptionBounds.width, descriptionBounds.height));
			}
			
			offset.y += descriptionBounds.height;
		}

		// Offset between items
		offset.y += ITEM_VERTICAL_MARGIN;
		
		// If item is expanded, draw children
		if (item.isExpanded()) {
			// Start children indention after check image
			int childIndent = checkImageData.width + ITEM_IMAGE_MARGIN;
			offset.x += childIndent;

			// Draw children
			for (DetailTreeItem child : item.getChildren()) {
				offset = paintItem(gc, offset, child, draw);
			}
			
			// Restore idention level
			offset.x -= childIndent;
		}
		// Otherwise, clear previously computed item regions for all children to
		// prevent it matching a hit test after an item is collapsed.
		else {
			for (DetailTreeItem child : item.getChildren()) {
				visitItems(child, clearRegionsVisitor);
			}
		}

		// Update default height
		defaultSize.y = offset.y;
		
		// Update item area
		itemArea.height = defaultSize.y - itemArea.y;
		itemArea.width = defaultSize.x - itemArea.x;
		item.setArea(itemArea);
		
		// Update the vertical scrolling increment to the height of the shortest item
		if ((verticalScrollIncrement == 0) || (itemArea.height < verticalScrollIncrement)) {
			verticalScrollIncrement = itemArea.height;
		}

		return offset;

	}

	/**
	 * Handle mouse button down.
	 * 
	 * @param x Horizontal coordinates
	 * @param y Vertical coordinates
	 */
	private void onMouseDown(final int x, final int y) {
		DetailTreeItem item = hitTest(x, y, ItemRegion.CHECKBOX);
		if (item == null) {
			item = hitTest(x, y, ItemRegion.TEXT);
		}
		if (item != null) {
			if (item != getSelectedItem()) {
				setSelectedItem(item);
				notifySelectionChanged();
			}
		}
	
		visitItems(new ItemVisitor() {
			@Override
			public boolean visit(DetailTreeItem item) {
				ItemRegion region = item.hitTest(x, y);
				if (region != null) {
					// Expand/collapse item
					if (region == ItemRegion.EXPAND) {
						toggleExpand(item);
						return false;
					}
					// Check/un-check item
					else if ((region == ItemRegion.CHECKBOX) ||
					(region == ItemRegion.TEXT)) {
						if ((item.getStyle() & SWT.CHECK) == SWT.CHECK) {
							item.setChecked(!item.isChecked(), true);
					}	
						return false;
					}
				}
				return true;
			}
		});
	}

	/**
	 * Toggles an item expansion.
	 * 
	 * @param item Item
	 */
	private void toggleExpand(DetailTreeItem item) {
		if (item.isExpanded()) {
			item.collapse();
		}
		else {
			item.expand();
		}
	}

	/**
	 * Visits all items.
	 * 
	 * @param v Visitor
	 */
	public void visitItems(ItemVisitor v) {
		for (DetailTreeItem item : getRootItems()) {
			if (!visitItems(item, v)) {
				break;
			}
		}
	}
	
	/**
	 * Visits an item and all of its child items.
	 * 
	 * @param item Item
	 * @param v Visitor
	 * @return <code>true</code> to continue visiting
	 */
	public boolean visitItems(DetailTreeItem item, ItemVisitor v) {
		if (!v.visit(item)) {
			return false;
		}
		for (DetailTreeItem child : item.getChildren()) {
			if (!visitItems(child, v)) {
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Paints the control.
	 * 
	 * @param gc Graphics context
	 */
	private void onPaint(GC gc) {
		Rectangle clientArea = getClientArea();
		
		GC gcDevice = gc;
		// Double-buffer drawing
		Image bufferImage = null;
		if ((getStyle() & SWT.DOUBLE_BUFFERED) == SWT.DOUBLE_BUFFERED) {
			bufferImage = new Image(getDisplay(), clientArea.width, clientArea.height);
			gc = new GC(bufferImage);
		}
		
		// Erase background
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		gc.fillRectangle(clientArea);

		// Initialize scroll offset
		Point offset = new Point(-scrollOffset.x, -scrollOffset.y);

		// Paint items
		for (DetailTreeItem item : getRootItems()) {
			offset = paintItem(gc, offset, item, true);
			
			// Draw separator
			if (getDrawSeparator()) {
				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
				gc.drawLine(clientArea.x, offset.y, clientArea.x + clientArea.width, offset.y);
			}
		}

		if (bufferImage != null) {
			gcDevice.drawImage(bufferImage, 0, 0);
			bufferImage.dispose();
		}
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

	@Override
	public ISelection getSelection() {
		return new StructuredSelection(getSelectedItem());
	}

	@Override
	public void setSelection(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection)selection;
			setSelectedItem(sel.isEmpty() ? null : (DetailTreeItem)sel.getFirstElement());
			notifySelectionChanged();
		}
	}

	/**
	 * Adds a new listener to selection change events.
	 * 
	 * @param listener Listener to add
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionListeners.add(listener);
	}
	
	/**
	 * Removes a listener from selection change events.
	 * 
	 * @param listener Listener to remove
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionListeners.remove(listener);
	}

	@Override
	public void addCheckStateListener(ICheckStateListener listener) {
		checkListeners.add(listener);
	}

	@Override
	public void removeCheckStateListener(ICheckStateListener listener) {
		checkListeners.remove(listener);
	}


	@Override
	public boolean getChecked(Object element) {
		return ((DetailTreeItem)element).isChecked();
	}

	@Override
	public boolean setChecked(Object element, boolean state) {
		DetailTreeItem item = (DetailTreeItem)element;
		item.setChecked(state);
		return item.isChecked() == state;
	}

	/**
	 * Notifies listeners of a selection changed.
	 */
	void notifySelectionChanged() {
		SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());
		Object[] listeners = selectionListeners.getListeners();
		for (Object listener : listeners) {
			try {
				((ISelectionChangedListener)listener).selectionChanged(event);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Notifies listeners of a checked state changed.
	 * 
	 * @param item Item that changed
	 */
	void notifyCheckStateChanged(DetailTreeItem item) {
		CheckStateChangedEvent event = new CheckStateChangedEvent(this, item, item.isChecked());
		Object[] listeners = checkListeners.getListeners();
		for (Object listener : listeners) {
			try {
				((ICheckStateListener)listener).checkStateChanged(event);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Tree item visitor.
	 */
	public interface ItemVisitor {
		/**
		 * Called to visit an item.
		 * 
		 * @param item Item
		 * @return <code>true</code> to continue visiting items.
		 */
		public boolean visit(DetailTreeItem item);
	}

	/** Visitor to clear item regions */
	private final static ItemVisitor clearRegionsVisitor = new ItemVisitor() {
		@Override
		public boolean visit(DetailTreeItem item) {
			item.clearRegions();
			return true;
		}
	};
}
