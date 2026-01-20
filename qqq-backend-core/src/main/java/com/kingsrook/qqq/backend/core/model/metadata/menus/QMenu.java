/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.model.metadata.menus;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** A list of items that can be selected by a user of a qqq application.
 *******************************************************************************/
public class QMenu implements QMetaDataObject, Cloneable, QMenuItemContainerInterface
{
   private String label;
   private QIcon  icon;

   private QMenuSlotInterface slot;

   private List<QMenuItemInterface> items;



   /*******************************************************************************
    * Getter for label
    * @see #withLabel(String)
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    * Setter for label
    * @see #withLabel(String)
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    * Fluent setter for label
    *
    * @param label
    * user-facing text to display as the label for this menu.  e.g., "File, Edit, View"
    * or "Actions",
    * @return this
    *******************************************************************************/
   public QMenu withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    * Getter for icon
    * @see #withIcon(QIcon)
    *******************************************************************************/
   public QIcon getIcon()
   {
      return (this.icon);
   }



   /*******************************************************************************
    * Setter for icon
    * @see #withIcon(QIcon)
    *******************************************************************************/
   public void setIcon(QIcon icon)
   {
      this.icon = icon;
   }



   /*******************************************************************************
    * Fluent setter for icon
    *
    * @param icon
    * Optional icon to display with this menu.
    * @return this
    *******************************************************************************/
   public QMenu withIcon(QIcon icon)
   {
      this.icon = icon;
      return (this);
   }



   /*******************************************************************************
    * Getter for slot
    * @see #withSlot(QMenuSlotInterface)
    *******************************************************************************/
   public QMenuSlotInterface getSlot()
   {
      return (this.slot);
   }



   /*******************************************************************************
    * Setter for slot
    * @see #withSlot(QMenuSlotInterface)
    *******************************************************************************/
   public void setSlot(QMenuSlotInterface slot)
   {
      this.slot = slot;
   }



   /*******************************************************************************
    * Fluent setter for slot
    *
    * @param slot
    * Where this menu is displayed.  e.g., on a query screen as the actions menu,
    * or on a view screen as an additional custom menu.
    * @return this
    *******************************************************************************/
   public QMenu withSlot(QMenuSlotInterface slot)
   {
      this.slot = slot;
      return (this);
   }



   /*******************************************************************************
    * Getter for items
    * @see #withItems(List)
    *******************************************************************************/
   public List<QMenuItemInterface> getItems()
   {
      return (this.items);
   }



   /*******************************************************************************
    * Setter for items
    * @see #withItems(List)
    *******************************************************************************/
   public void setItems(List<QMenuItemInterface> items)
   {
      this.items = items;
   }



   /*******************************************************************************
    * Fluent setter for items
    *
    * @param items
    * Contents of the menu.
    * @return this
    *******************************************************************************/
   public QMenu withItems(List<QMenuItemInterface> items)
   {
      this.items = items;
      return (this);
   }



   /*******************************************************************************
    * Fluently add a single item
    *
    * @param item
    * one item to add to the menu.
    * @return this
    *******************************************************************************/
   public QMenu withItem(QMenuItemInterface item)
   {
      if(this.items == null)
      {
         this.items = new ArrayList<>();
      }
      this.items.add(item);
      return (this);
   }



   /***************************************************************************
    * As part of QInstanceValidation, verify that the meta-data in this object
    * is all fully valid.
    *
    * <p>Subclasses should generally include a call to super.validate</p>
    ***************************************************************************/
   public void validate(QInstanceValidator validator, QInstance qInstance, QMetaDataObject parentObject)
   {
      validator.assertCondition(slot != null, "Missing a slot for menu item [" + getLabel() + "] in " + parentObject);
      for(QMenuItemInterface menuItem : CollectionUtils.nonNullList(items))
      {
         menuItem.validate(validator, qInstance, parentObject);
      }
   }



   /***************************************************************************
    * Creates and returns a deep copy of this menu.
    *
    * <p>The cloned menu will have its own copy of the items list, with each
    * item also being cloned. This ensures that modifications to the cloned
    * menu or its items will not affect the original.</p>
    *
    * @return a deep clone of this menu
    * @throws RuntimeException if cloning is not supported (should not occur)
    ***************************************************************************/
   @Override
   public QMenu clone()
   {
      try
      {
         QMenu clone = (QMenu) super.clone();

         if(items != null)
         {
            List<QMenuItemInterface> cloneItems = new ArrayList<>();
            for(QMenuItemInterface item : items)
            {
               cloneItems.add(item.clone());
            }
            clone.setItems(cloneItems);
         }

         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new RuntimeException(e);
      }
   }
}
