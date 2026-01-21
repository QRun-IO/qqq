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


/*******************************************************************************
 ** Menu item that represents a visual divider or separator in a menu.
 **
 ** <p>Dividers are used to visually separate groups of menu items within
 ** a menu structure. They typically render as horizontal lines or spacing
 ** in the user interface.</p>
 **
 ** <p>Dividers do not have labels, icons, or any interactive behavior - they
 ** are purely visual elements for menu organization.  Any settings from the
 * base class that are populated in objects of this type will probably
 * be ignored by any UI.</p>
 *******************************************************************************/
public class QMenuItemDivider extends QMenuItemBase
{

   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public QMenuItemDivider()
   {
   }



   /***************************************************************************
    * Returns the item type identifier for divider menu items.
    *
    * @return always returns "DIVIDER"
    ***************************************************************************/
   @Override
   public String getItemType()
   {
      return ("DIVIDER");
   }

}