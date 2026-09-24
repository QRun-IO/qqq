/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.metadata;


import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** a default implementation of MetaDataActionCustomizerInterface, that is all noop.
 *******************************************************************************/
public class DefaultNoopMetaDataActionCustomizer implements MetaDataActionCustomizerInterface
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public boolean allowTable(MetaDataInput input, QTableMetaData table)
   {
      return (true);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public boolean allowProcess(MetaDataInput input, QProcessMetaData process)
   {
      return (true);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public boolean allowReport(MetaDataInput input, QReportMetaData report)
   {
      return (true);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public boolean allowApp(MetaDataInput input, QAppMetaData app)
   {
      return (true);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public boolean allowWidget(MetaDataInput input, QWidgetMetaDataInterface widget)
   {
      return (true);
   }

}
