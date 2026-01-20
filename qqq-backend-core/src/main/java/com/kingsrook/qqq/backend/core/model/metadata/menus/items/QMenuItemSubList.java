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
import com.kingsrook.qqq.backend.core.model.metadata.menus.QMenuItemContainerInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 ** Menu item that contains a sub-list of other menu items.
 **
 ** <p>Sub-lists are used to group related menu items together within a menu.
 ** They differ from sub-menus in that they typically render as a flat list
 ** rather than a nested menu structure. Sub-lists are useful for organizing
 ** items that belong together logically but should appear inline rather than
 ** in a separate menu - from the user point of view, sublists are invisible.
 ** They are only visible to developers.</p>
 **
 ** <p>This class implements {@link QMenuItemContainerInterface} to allow
 ** it to contain other menu items.</p>
 **
 ** @see QMenuItemSubMenu
 ** @see QMenuItemContainerInterface
 *******************************************************************************/
public class QMenuItemSubList extends QMenuItemBase implements QMenuItemContainerInterface
{
   private ArrayList<QMenuItemInterface> items;



   /***************************************************************************
    * Returns the item type identifier for sub-list menu items.
    *
    * @return always returns "SUB_LIST"
    ***************************************************************************/
   @Override
   public String getItemType()
   {
      return "SUB_LIST";
   }



   /***************************************************************************
    * Returns the values map containing the list of items in this sub-list.
    *
    * @return a map containing the items list under the key "items"
    ***************************************************************************/
   @Override
   public Map<String, Serializable> getValues()
   {
      return MapBuilder.of("items", items);
   }



   /***************************************************************************
    * Validates all menu items contained within this sub-list.
    *
    * <p>Recursively validates each item in the sub-list, passing this sub-list
    * as the parent object for validation context.</p>
    *
    * @param validator the validator instance to use for reporting errors
    * @param qInstance the QQQ instance being validated
    * @param parentObject the parent metadata object containing this sub-list
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
    * Contents of the sub list
    * @return this
    *******************************************************************************/
   public QMenuItemSubList withItems(List<QMenuItemInterface> items)
   {
      setItems(items);
      return (this);
   }



   /*******************************************************************************
    * Fluently add a single item
    *
    * @param item
    * one item to add to this sub list
    * @return this
    *******************************************************************************/
   public QMenuItemSubList withItem(QMenuItemInterface item)
   {
      if(this.items == null)
      {
         this.items = new ArrayList<>();
      }
      this.items.add(item);
      return (this);
   }



   /***************************************************************************
    * Creates and returns a deep copy of this sub-list menu item.
    *
    * <p>The cloned sub-list will have its own copy of the items list, with
    * each item also being cloned. This ensures that modifications to the
    * cloned sub-list or its items will not affect the original.</p>
    *
    * @return a deep clone of this sub-list menu item
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
