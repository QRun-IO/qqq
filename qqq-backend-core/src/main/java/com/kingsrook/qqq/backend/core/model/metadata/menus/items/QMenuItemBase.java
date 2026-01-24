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


import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;


/*******************************************************************************
 * abstract base class for menu items.  provides fields for basic methods
 * required by the interface
 *******************************************************************************/
public abstract class QMenuItemBase implements QMenuItemInterface
{
   private String label;
   private QIcon  icon;



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
    * user-facing text to display as the label for this menu item.  e.g., "New", "Copy",
    * or "Process Orders".
    * @return this
    *******************************************************************************/
   public QMenuItemBase withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /***************************************************************************
    * Creates and returns a shallow copy of this menu item.
    *
    * <p>The cloned item will have its own copy of the label and icon fields.
    * Subclasses should override this method to provide deep cloning if they
    * contain additional mutable state.</p>
    *
    * @return a clone of this menu item
    * @throws RuntimeException if cloning is not supported (should not occur)
    ***************************************************************************/
   @Override
   public QMenuItemBase clone()
   {
      try
      {
         QMenuItemBase clone = (QMenuItemBase) super.clone();
         if(getIcon() != null)
         {
            clone.setIcon(getIcon().clone());
         }

         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new RuntimeException(e);
      }
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
    * Optional icon to display with this menu item.
    *
    * @return this
    *******************************************************************************/
   public QMenuItemBase withIcon(QIcon icon)
   {
      this.icon = icon;
      return (this);
   }

}
