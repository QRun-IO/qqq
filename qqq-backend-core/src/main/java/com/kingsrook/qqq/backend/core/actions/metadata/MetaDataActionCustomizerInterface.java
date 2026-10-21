/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.metadata;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataOutput;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Interface for customizations that can be injected by an application into
 ** the MetaDataAction - e.g., loading applicable meta-data for a user into a
 ** frontend.
 *******************************************************************************/
public interface MetaDataActionCustomizerInterface
{

   /***************************************************************************
    **
    ***************************************************************************/
   boolean allowTable(MetaDataInput input, QTableMetaData table);

   /***************************************************************************
    **
    ***************************************************************************/
   boolean allowProcess(MetaDataInput input, QProcessMetaData process);

   /***************************************************************************
    **
    ***************************************************************************/
   boolean allowReport(MetaDataInput input, QReportMetaData report);

   /***************************************************************************
    **
    ***************************************************************************/
   boolean allowApp(MetaDataInput input, QAppMetaData app);

   /***************************************************************************
    **
    ***************************************************************************/
   boolean allowWidget(MetaDataInput input, QWidgetMetaDataInterface widget);

   /***************************************************************************
    **
    ***************************************************************************/
   default void postProcess(MetaDataOutput metaDataOutput) throws QException
   {
      /////////////////////
      // noop by default //
      /////////////////////
   }

}
