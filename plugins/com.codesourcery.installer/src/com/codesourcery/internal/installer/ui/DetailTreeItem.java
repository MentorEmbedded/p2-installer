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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;

import com.codesourcery.internal.installer.ui.DetailTree.ItemVisitor;

/**
 * Item for detail tree.
 */
public class DetailTreeItem {
	/** No items constant */
	private static final DetailTreeItem[] NO_ITEMS = new DetailTreeItem[0];
	
	/**
	 * Item regions
	 */
	enum ItemRegion {
		/** Expand area */
		EXPAND,
		/** Check-box area */
		CHECKBOX,
		/** Text area */
		TEXT,
		/** Description area */
		DESCRIPTION
	}

	/** Style flags */
	private int style;
	/** Tree for item */
	private DetailTree tree;
	/** Parent item or <code>null</code> */
	private DetailTreeItem parent;
	/** Item text */
	private String text;
	/** Item description */
	private String description;
	/** Item data */
	private Object data;
	/** Item child items */
	private DetailTreeItem[] children = NO_ITEMS;
	/** Item area */
	private Rectangle area;
	/** Item regions */
	private Rectangle[] regions = new Rectangle[ItemRegion.values().length];
	/** <code>true</code> if item is expanded */
	private boolean expanded = false;
	/** <code>true</code> is item is checked */
	private boolean checked;

	/**
	 * Constructor
	 * 
	 * @param tree Tree for item
	 * @param style SWT.CHECK for check-box item
	 */
	public DetailTreeItem(DetailTree tree, int style) {
		this.tree = tree;
		this.style = style;
		this.parent = null;
		tree.addItem(this);
	}

	/**
	 * Constructor
	 * 
	 * @param item Parent item
	 * @param style SWT.CHECK for check-box item
	 */
	public DetailTreeItem(DetailTreeItem item, int style) {
		this.tree = item.getTree();
		this.style = style;
		this.parent = item;
		item.addChild(this);
		// If parent is checked, this item is also checked
		if (item.isChecked()) {
			this.checked = true;
		}
	}

	/**
	 * Returns the item style.
	 * 
	 * @return Style
	 */
	public int getStyle() {
		return style;
	}

	/**
	 * Returns the tree.
	 * 
	 * @return Tree
	 */
	public DetailTree getTree() {
		return tree;
	}

	/**
	 * Returns the parent item.
	 * 
	 * @return Parent item or <code>null</code>
	 */
	public DetailTreeItem getParent() {
		return parent;
	}
	
	/**
	 * Returns if the item has children.
	 * 
	 * @return <code>true</code> if item has children
	 */
	public boolean hasChildren() {
		return (children.length > 0);
	}
	
	/**
	 * Returns the item children.
	 * 
	 * @return Children
	 */
	public DetailTreeItem[] getChildren() {
		return children; 
	}
	
	/**
	 * Adds a new child item.
	 * 
	 * @param child Child item
	 */
	void addChild(DetailTreeItem child) {
		DetailTreeItem[] newItems = new DetailTreeItem[children.length + 1];
		System.arraycopy(children, 0, newItems, 0, children.length);
		newItems[newItems.length - 1] = child;
		children = newItems;
	}

	/**
	 * Checks an item and sends a notification.
	 * 
	 * @param checked <code>true</code> to check, <code>false</code> to un-check
	 * @param notify <code>true</code> to send notification
	 */
	private void check(boolean checked, boolean notify) {
		if (this.checked != checked) {
			this.checked = checked;
			getTree().redraw(this, ItemRegion.CHECKBOX);
			if (notify) {
				getTree().notifyCheckStateChanged(this);
			}
		}
	}

	/**
	 * Sets the item checked/un-checked.
	 * 
	 * @param checked <code>true</code> if checked
	 */
	public void setChecked(final boolean checked) {
		setChecked(checked, false);
	}
	
	/**
	 * Sets the item checked/un-checked.
	 * 
	 * @param checked <code>true</code> if checked
	 * @param notify <code>true</code> to send notification
	 */
	void setChecked(final boolean checked, final boolean notify) {
		if (isChecked() != checked) {
			// Check item and all child items
			getTree().visitItems(this, new ItemVisitor() {
				@Override
				public boolean visit(DetailTreeItem item) {
					item.check(checked, notify);
					return true;
				}
			});

			// Checked
			if (checked) {
				// If all child items of parent item are checked,
				// check the parent item
				if (getParent() != null) {
					boolean allChecked = true;
					DetailTreeItem[] parentItems = getParent().getChildren();
					for (DetailTreeItem parentItem : parentItems) {
						if (((parentItem.getStyle() & SWT.CHECK) == SWT.CHECK) && 
								!parentItem.isChecked()) {
							allChecked = false;
							break;
						}
					}
					if (allChecked) {
						getParent().check(true, notify);
						getTree().redraw(getParent(), ItemRegion.CHECKBOX);
					}
				}
				
			}
			// Un-checked
			else {
				// Un-check all parent items
				DetailTreeItem iter = this;
				while (iter != null) {
					iter.check(false, notify);
					iter = iter.getParent();
				}
			}
		}
	}

	/**
	 * Returns if the item is checked.
	 * 
	 * @return <code>true</code> if checked
	 */
	public boolean isChecked() {
		return checked;
	}

	/**
	 * Sets the item area.
	 * 
	 * @param area Item area
	 */
	void setArea(Rectangle area) {
		this.area = area;
	}
	
	/**
	 * Returns the item area.
	 * 
	 * @return Item area
	 */
	Rectangle getArea() {
		return area;
	}

	/**
	 * Returns the region that corresponds to mouse coordinates.
	 * 
	 * @param x Horizontal coordinate
	 * @param y Vertical coordinate
	 * @return Item region or <code>null</code>
	 */
	ItemRegion hitTest(int x, int y) {
		ItemRegion region = null;
		
		for (int index = 0; index < regions.length; index ++) {
			if ((regions[index] != null) && regions[index].contains(x, y)) {
				region = ItemRegion.values()[index];
				break;
			}
		}
		
		return region;
	}

	/**
	 * Returns the area for a region.
	 * 
	 * @param region Region
	 * @return Area or <code>null</code>
	 */
	Rectangle getRegion(ItemRegion region) {
		return regions[region.ordinal()];
	}
	
	/**
	 * Clears all item regions.
	 */
	void clearRegions() {
		for (int index = 0; index < regions.length; index ++) {
			regions[index] = null;
		}
	}

	/**
	 * Sets an item region.
	 * 
	 * @param region Region
	 * @param area Region area
	 */
	void setRegion(ItemRegion region, Rectangle area) {
		regions[region.ordinal()] = area;
	}

	/**
	 * Returns if the item is revealed (a child of expanded parents).
	 * 
	 * @return <code>true</code> if item is revealed
	 */
	boolean isRevealed() {
		DetailTreeItem parent = getParent();
		while (parent != null) {
			if (!parent.isExpanded())
				return false;
			parent = parent.getParent();
		}
		return true;
	}

	/**
	 * Expands the item if it is collapsed.
	 */
	public void expand() {
		if (!isExpanded()) {
			this.expanded = true;
			getTree().recalcScrollBars();
			getTree().redraw();
		}
	}
	
	/**
	 * Expands the item and all its children.
	 */
	public void expandAll() {
		expand();
		DetailTreeItem[] children = getChildren();
		for (DetailTreeItem child : children) {
			child.expandAll();
		}
	}

	/**
	 * Collapses the item if is expanded.
	 */
	public void collapse() {
		if (isExpanded()) {
			this.expanded = false;
			getTree().recalcScrollBars();
			getTree().redraw();
		}
	}
	
	/**
	 * Collapses the item and all its children.
	 */
	public void collapseAll() {
		collapse();
		DetailTreeItem[] children = getChildren();
		for (DetailTreeItem child : children) {
			child.collapseAll();
		}
	}
	
	/**
	 * Returns if the item is expanded.
	 * 
	 * @return <code>true</code> if item is expanded
	 */
	public boolean isExpanded() {
		return expanded;
	}

	/**
	 * Sets the item text.
	 * 
	 * @param text Text
	 */
	public void setText(String text) {
		this.text = text;
		
		getTree().recalcScrollBars();
		getTree().redraw(this, ItemRegion.TEXT);
	}
	
	/**
	 * Returns the item text.
	 * 
	 * @return Item text
	 */
	public String getText() {
		return text;
	}

	/**
	 * Sets the item description.
	 * 
	 * @param description Description
	 */
	public void setDescription(String description) {
		this.description = description;

		getTree().recalcScrollBars();
		getTree().redraw(this, ItemRegion.DESCRIPTION);
	}
	
	/**
	 * Returns the item description.
	 * 
	 * @return Description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Sets data for the item.
	 * 
	 * @param data Data or <code>null</code>
	 */
	public void setData(Object data) {
		this.data = data;
	}
	
	/**
	 * Returns data for the item.
	 * 
	 * @return Data or <code>null</code>
	 */
	public Object getData() {
		return data;
	}
}
