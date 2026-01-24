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

package com.kingsrook.qqq.backend.core.model.metadata.menus.items;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.gson.reflect.TypeToken;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.menus.QMenuItemContainerInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 ** Menu item that contains a nested sub-menu of other menu items.
 **
 ** <p>Sub-menus are used to create hierarchical menu structures. They differ
 ** from sub-lists in that they typically render as a nested menu that opens
 ** when selected, rather than displaying items inline. Sub-menus are useful
 ** for organizing large numbers of menu items into logical groups for users
 ** to see.</p>
 **
 ** <p>This class implements {@link QMenuItemContainerInterface} to allow
 ** it to contain other menu items. Sub-menus can have their own label and
 ** icon, which are displayed in the parent menu.</p>
 **
 ** @see QMenuItemSubList
 ** @see QMenuItemContainerInterface
 *******************************************************************************/
public class QMenuItemSubMenu extends QMenuItemBase implements QMenuItemContainerInterface
{
   private ArrayList<QMenuItemInterface> items;



   /***************************************************************************
    * Returns the item type identifier for sub-menu menu items.
    *
    * @return always returns "SUB_MENU"
    ***************************************************************************/
   @Override
   public String getItemType()
   {
      return "SUB_MENU";
   }



   /***************************************************************************
    * Returns the values map containing the list of items in this sub-menu.
    *
    * @return a map containing the items list under the key "items"
    ***************************************************************************/
   @Override
   public Map<String, Serializable> getValues()
   {
      return MapBuilder.of("items", items);
   }



   /***************************************************************************
    * Validates all menu items contained within this sub-menu.
    *
    * <p>Recursively validates each item in the sub-menu, passing this sub-menu
    * as the parent object for validation context.</p>
    *
    * @param validator the validator instance to use for reporting errors
    * @param qInstance the QQQ instance being validated
    * @param parentObject the parent metadata object containing this sub-menu
    ***************************************************************************/
   @Override
   public void validate(QInstanceValidator validator, QInstance qInstance, QMetaDataObject parentObject)
   {
      super.validate(validator, qInstance, parentObject);

      for(QMenuItemInterface qMenuItemInterface : CollectionUtils.nonNullList(items))
      {
         qMenuItemInterface.validate(validator, qInstance, this);
      }
   }



   /***************************************************************************
    * Fluent setter for label that returns this sub-menu for method chaining.
    *
    * @param label user-facing text to display as the label for this sub-menu
    * @return this sub-menu instance
    ***************************************************************************/
   @Override
   public QMenuItemSubMenu withLabel(String label)
   {
      super.withLabel(label);
      return this;
   }



   /***************************************************************************
    * Fluent setter for icon that returns this sub-menu for method chaining.
    *
    * @param icon optional icon to display with this sub-menu
    * @return this sub-menu instance
    ***************************************************************************/
   @Override
   public QMenuItemSubMenu withIcon(QIcon icon)
   {
      super.withIcon(icon);
      return this;
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
      this.items = CollectionUtils.useOrWrap(items, new TypeToken<>() {});
   }



   /*******************************************************************************
    * Fluent setter for items
    *
    * @param items
    * Contents of the sub Menu
    * @return this
    *******************************************************************************/
   public QMenuItemSubMenu withItems(List<QMenuItemInterface> items)
   {
      setItems(items);
      return (this);
   }



   /*******************************************************************************
    * Fluently add a single item
    *
    * @param item
    * one item to add to this sub Menu
    * @return this
    *******************************************************************************/
   public QMenuItemSubMenu withItem(QMenuItemInterface item)
   {
      if(this.items == null)
      {
         this.items = new ArrayList<>();
      }
      this.items.add(item);
      return (this);
   }



   /***************************************************************************
    * Creates and returns a deep copy of this sub-menu menu item.
    *
    * <p>The cloned sub-menu will have its own copy of the items list, with
    * each item also being cloned. This ensures that modifications to the
    * cloned sub-menu or its items will not affect the original.</p>
    *
    * @return a deep clone of this sub-menu menu item
    ***************************************************************************/
   @Override
   public QMenuItemSubList clone()
   {
      QMenuItemSubList clone = (QMenuItemSubList) super.clone();

      if(items != null)
      {
         ArrayList<QMenuItemInterface> cloneItems = new ArrayList<>();
         for(QMenuItemInterface item : items)
         {
            cloneItems.add(item.clone());
         }
         clone.setItems(cloneItems);
      }

      return clone;
   }
}
