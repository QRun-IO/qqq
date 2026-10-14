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

package com.kingsrook.qqq.backend.core.model.developer;


import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** Contributes a panel to a record's developer view.
 **
 ** Any module or qbit can provide one: implement this interface, and register
 ** the class in the instance with
 ** {@code DeveloperPanelsMetaData.ofOrWithNew(qInstance).withProvider(new QCodeReference(MyProvider.class))}
 ** (e.g., from a MetaDataProducer or a qbit's produce method).
 **
 ** For each request, {@link #isAvailable} is asked first; only when it returns
 ** true is {@link #build} called.  Providers must only reveal what the current
 ** user (QContext.getQSession()) is allowed to see - the record in the context
 ** was already read under the user's table permissions and record security
 ** locks, but anything else a provider reads (other tables, processes,
 ** external systems) must check its own permissions.  A provider that throws
 ** gets a panel with an error message; the other panels are unaffected.
 *******************************************************************************/
public interface DeveloperPanelProviderInterface
{

   /*******************************************************************************
    ** Stable identifier of the panel, unique in the instance (e.g., "esb").
    *******************************************************************************/
   String getId();


   /*******************************************************************************
    ** Heading of the panel.
    *******************************************************************************/
   String getTitle();


   /*******************************************************************************
    ** Sort order among panels (lower first).  Built-in panels use 100-199;
    ** qbits default to 500.
    *******************************************************************************/
   default Integer getOrder()
   {
      return (500);
   }


   /*******************************************************************************
    ** Optional icon-name hint for the frontend (a Material icon name).
    *******************************************************************************/
   default String getIcon()
   {
      return (null);
   }


   /*******************************************************************************
    ** Whether this provider has anything to show for the context's table (and
    ** record) for the current user - e.g., the module is installed and active
    ** for the table, and the user may use it.
    *******************************************************************************/
   boolean isAvailable(DeveloperPanelContext context) throws QException;


   /*******************************************************************************
    ** Build the panel's content.  The id, title, order and icon are filled in
    ** from this provider when the returned panel leaves them null.
    *******************************************************************************/
   DeveloperPanel build(DeveloperPanelContext context) throws QException;

}
