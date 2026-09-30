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
