/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.qbits;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Interface for configuration settings used both in the production of meta-data
 ** for a QBit, but also at runtime, e.g., to be aware of exactly how the qbit
 ** has been incorporated into an application.
 **
 ** For example:
 ** - should the QBit define certain tables, or will they be supplied by the application?
 ** - what other meta-data names should the qbit reference (backends, schedulers)
 ** - what meta-data-customizer(s) should be used?
 **
 ** When implementing a QBit, you'll implement this interface - adding whatever
 ** (if any) properties you need, and if you have any rules, then overriding
 ** the validate method (ideally the one that takes the List-of-String errors)
 **
 ** When using a QBit, you'll create an instance of the QBit's config object,
 ** and pass it through to the QBit producer.
 *******************************************************************************/
public interface QBitConfig extends Serializable
{
   QLogger LOG = QLogger.getLogger(QBitConfig.class);


   /***************************************************************************
    **
    ***************************************************************************/
   default void validate(QInstance qInstance) throws QBitConfigValidationException
   {
      List<String> errors = new ArrayList<>();

      try
      {
         validate(qInstance, errors);
      }
      catch(Exception e)
      {
         LOG.warn("Error validating QBitConfig: " + this.getClass().getName(), e);
      }

      if(!errors.isEmpty())
      {
         throw (new QBitConfigValidationException(this, errors));
      }
   }


   /***************************************************************************
    **
    ***************************************************************************/
   default void validate(QInstance qInstance, List<String> errors)
   {
      /////////////////////////////////////
      // nothing to validate by default! //
      /////////////////////////////////////
   }


   /***************************************************************************
    **
    ***************************************************************************/
   default boolean assertCondition(boolean condition, String message, List<String> errors)
   {
      if(!condition)
      {
         errors.add(message);
      }
      return (condition);
   }


   /***************************************************************************
    **
    ***************************************************************************/
   default MetaDataCustomizerInterface<QTableMetaData> getTableMetaDataCustomizer()
   {
      return (null);
   }


   /***************************************************************************
    *
    ***************************************************************************/
   default String getDefaultBackendNameForTables()
   {
      return (null);
   }

}
