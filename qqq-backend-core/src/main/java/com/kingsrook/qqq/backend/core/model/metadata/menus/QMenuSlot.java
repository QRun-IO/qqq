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


/*******************************************************************************
 ** Enumeration of standard menu slot locations where menus can be displayed
 ** in a QQQ application.
 **
 ** <p>Menus are positioned in specific "slots" within the application UI.
 ** Each slot represents a different location and context where a menu can
 ** appear. This enum provides the core-known slot values that are recognized
 ** by the QQQ framework.</p>
 **
 ** <p>Slots are categorized by screen type (VIEW_SCREEN vs QUERY_SCREEN) and
 ** purpose (ACTIONS vs ADDITIONAL). The ACTIONS slots typically contain
 ** primary actions for the screen, while ADDITIONAL slots are for
 ** supplementary menus.</p>
 **
 ** @see QMenuSlotInterface
 ** @see QMenu#withSlot(QMenuSlotInterface)
 *******************************************************************************/
public enum QMenuSlot implements QMenuSlotInterface
{
   /** Menu slot for primary actions on a record view screen */
   VIEW_SCREEN_ACTIONS,

   /** Menu slot for additional/custom menus on a record view screen. */
   VIEW_SCREEN_ADDITIONAL,

   /** Menu slot for primary actions on a query screen (list/search screen). */
   QUERY_SCREEN_ACTIONS,

   /** Menu slot for additional/custom menus on a query screen. */
   QUERY_SCREEN_ADDITIONAL
}
