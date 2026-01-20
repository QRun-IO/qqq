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

package com.kingsrook.qqq.backend.core.model.metadata.menus.adjusters;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemBuiltIn;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemInterface;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.utils.BackendQueryFilterUtils;


/*******************************************************************************
 ** Utility class for matching menu items based on various criteria.
 **
 ** <p>This class provides a flexible way to identify menu items within a menu
 ** structure. It supports matching by:</p>
 ** <ul>
 **   <li>Exact item instance equality</li>
 **   <li>Built-in option type (for QMenuItemBuiltIn items)</li>
 **   <li>Item class type</li>
 **   <li>Label text using filter criteria operators</li>
 ** </ul>
 **
 ** <p>Used primarily by {@link QMenuAdjuster} to locate items for modification
 ** or removal operations.</p>
 **
 ** @see QMenuAdjuster
 *******************************************************************************/
public class QMenuItemMatcher
{
   private QMenuItemInterface                      item;
   private QMenuItemBuiltIn.BuiltInOptionInterface builtInOption;
   private Class<? extends QMenuItemInterface>     menuItemClass;
   private QFilterCriteria                         labelCriteria;



   /*******************************************************************************
    ** Constructor that matches items by exact instance equality.
    **
    ** @param item the specific menu item instance to match
    *******************************************************************************/
   public QMenuItemMatcher(QMenuItemInterface item)
   {
      this.item = item;
   }



   /*******************************************************************************
    ** Constructor that matches built-in menu items by their option type.
    **
    ** @param builtInOption the built-in option to match (e.g., NEW, EDIT, DELETE)
    *******************************************************************************/
   public QMenuItemMatcher(QMenuItemBuiltIn.BuiltInOptionInterface builtInOption)
   {
      this.builtInOption = builtInOption;
   }



   /*******************************************************************************
    ** Constructor that matches items by their class type.
    **
    ** @param menuItemClass the class type to match (e.g., QMenuItemDivider.class)
    *******************************************************************************/
   public QMenuItemMatcher(Class<? extends QMenuItemInterface> menuItemClass)
   {
      this.menuItemClass = menuItemClass;
   }



   /*******************************************************************************
    ** Constructor that matches items by label (doing an EQUALS match)
    **
    ** @param labelValue the label value to match against
    *******************************************************************************/
   public QMenuItemMatcher(String labelValue)
   {
      this(QCriteriaOperator.EQUALS, labelValue);
   }



   /*******************************************************************************
    ** Constructor that matches items by label text using a filter criteria operator.
    **
    ** @param operator the comparison operator (e.g., EQUALS, CONTAINS)
    ** @param labelValue the label value to match against
    *******************************************************************************/
   public QMenuItemMatcher(QCriteriaOperator operator, String labelValue)
   {
      this.labelCriteria = new QFilterCriteria("label", operator, labelValue);
   }



   /***************************************************************************
    * Determines whether the given menu item matches this matcher's criteria.
    *
    * <p>The method checks the item against all configured matching criteria
    * (instance, built-in option, class type, or label). Returns true if any
    * of the configured criteria match.</p>
    *
    * @param item the menu item to test
    * @return true if the item matches this matcher's criteria, false otherwise
    ***************************************************************************/
   public boolean doesItemMatch(QMenuItemInterface item)
   {
      if(this.item != null)
      {
         if(Objects.equals(this.item, item))
         {
            return (true);
         }
      }
      else if(builtInOption != null)
      {
         if(item instanceof QMenuItemBuiltIn builtIn && Objects.equals(builtIn.getOption(), builtInOption))
         {
            return (true);
         }
      }
      else if(menuItemClass != null && menuItemClass.isInstance(item))
      {
         return (true);
      }
      else if(labelCriteria != null && BackendQueryFilterUtils.doesCriteriaMatch(labelCriteria, "label", item.getLabel()))
      {
         return (true);
      }

      return (false);
   }
}
