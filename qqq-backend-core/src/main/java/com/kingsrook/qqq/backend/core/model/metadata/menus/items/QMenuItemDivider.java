/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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