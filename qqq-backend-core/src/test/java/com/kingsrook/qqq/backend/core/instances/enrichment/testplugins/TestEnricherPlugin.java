/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.instances.enrichment.testplugins;


import com.kingsrook.qqq.backend.core.instances.enrichment.plugins.QInstanceEnricherPluginInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 **
 *******************************************************************************/
public class TestEnricherPlugin implements QInstanceEnricherPluginInterface<QFieldMetaData>
{
   private final boolean isEnabled;


   /*******************************************************************************
    * Default Constructor - makes plugin be enabled
    *
    *******************************************************************************/
   public TestEnricherPlugin()
   {
      this(true);
   }


   /*******************************************************************************
    * Constructor
    *
    * @param isEnabled - if false, the plugin will not be enabled, so will not run.
    *******************************************************************************/
   public TestEnricherPlugin(boolean isEnabled)
   {
      this.isEnabled = isEnabled;
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public boolean isEnabled()
   {
      return isEnabled;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void enrich(QFieldMetaData field, QInstance qInstance)
   {
      if(field != null)
      {
         field.setLabel(field.getLabel() + " Plugged");
      }
   }

}
