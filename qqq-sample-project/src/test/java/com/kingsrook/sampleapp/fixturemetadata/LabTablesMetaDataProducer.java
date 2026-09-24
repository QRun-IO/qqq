/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.sampleapp.fixturemetadata;


import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** One scanned producer returning two independently usable sample tables.
 *******************************************************************************/
public class LabTablesMetaDataProducer implements MetaDataProducerInterface<MetaDataProducerMultiOutput>
{
   public static final String NAME = "metadataLabTables";



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance instance)
   {
      MetaDataProducerMultiOutput output = new MetaDataProducerMultiOutput();
      for(String name : new String[] { NAME + "First", NAME + "Second" })
      {
         output.add(new QTableMetaData().withName(name).withBackendName("metadataLab")
            .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
            .withField(new QFieldMetaData("name", QFieldType.STRING)));
      }
      return output;
   }
}
