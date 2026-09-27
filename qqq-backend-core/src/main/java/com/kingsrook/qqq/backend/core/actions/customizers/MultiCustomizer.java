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

package com.kingsrook.qqq.backend.core.actions.customizers;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.InitializableViaCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceWithProperties;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 ** Implementation of TableCustomizerInterface that runs multiple other customizers
 *******************************************************************************/
public class MultiCustomizer implements InitializableViaCodeReference, TableCustomizerInterface
{
   private static final String KEY_CODE_REFERENCES = "codeReferences";

   private List<TableCustomizerInterface> customizers = new ArrayList<>();


   /***************************************************************************
    * Factory method that builds a {@link QCodeReferenceWithProperties} that will
    * allow this multi-customizer to be assigned to a table, and to track
    * in that code ref's properties, the "sub" QCodeReferences to be used.
    *
    * Added to a table as in:
    * <pre>
    * table.withCustomizer(TableCustomizers.POST_INSERT_RECORD,
    *    MultiCustomizer.of(QCodeReference(x), QCodeReference(y)));
    * </pre>
    *
    * @param codeReferences
    * one or more {@link QCodeReference objects} to run when this customizer
    * runs.  note that they will run in the order provided in this list.
    ***************************************************************************/
   public static QCodeReferenceWithProperties of(QCodeReference... codeReferences)
   {
      ArrayList<QCodeReference> list = new ArrayList<>(Arrays.stream(codeReferences).toList());
      return (new QCodeReferenceWithProperties(MultiCustomizer.class, MapBuilder.of(KEY_CODE_REFERENCES, list)));
   }


   /***************************************************************************
    * Add an additional table customizer code reference to an existing
    * codeReference, e.g., constructed by the `of` factory method.
    *
    * @see #of(QCodeReference...)
    ***************************************************************************/
   public static void addTableCustomizer(QCodeReferenceWithProperties existingMultiCustomizerCodeReference, QCodeReference codeReference)
   {
      ArrayList<QCodeReference> list = (ArrayList<QCodeReference>) existingMultiCustomizerCodeReference.getProperties().computeIfAbsent(KEY_CODE_REFERENCES, key -> new ArrayList<>());
      list.add(codeReference);
   }



   /***************************************************************************
    * When this class is instantiated by the QCodeLoader, initialize the
    * sub-customizer objects.
    ***************************************************************************/
   @Override
   public void initialize(QCodeReference codeReference)
   {
      if(codeReference instanceof QCodeReferenceWithProperties codeReferenceWithProperties)
      {
         Serializable codeReferencesPropertyValue = codeReferenceWithProperties.getProperties().get(KEY_CODE_REFERENCES);
         if(codeReferencesPropertyValue instanceof List<?> list)
         {
            for(Object o : list)
            {
               if(o instanceof QCodeReference reference)
               {
                  TableCustomizerInterface customizer = QCodeLoader.getAdHoc(TableCustomizerInterface.class, reference);
                  customizers.add(customizer);
               }
            }
         }
         else
         {
            LOG.warn("Property KEY_CODE_REFERENCES [" + KEY_CODE_REFERENCES + "] must be a List<QCodeReference>.");
         }
      }

      if(customizers.isEmpty())
      {
         LOG.info("No TableCustomizers were specified for MultiCustomizer.");
      }
   }



   /***************************************************************************
    * run postQuery method over all sub-customizers
    ***************************************************************************/
   @Override
   public List<QRecord> postQuery(QueryOrGetInputInterface queryInput, List<QRecord> records) throws QException
   {
      for(TableCustomizerInterface customizer : customizers)
      {
         records = customizer.postQuery(queryInput, records);
      }
      return records;
   }



   /***************************************************************************
    * run preInsert method over all sub-customizers
    ***************************************************************************/
   @Override
   public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
   {
      for(TableCustomizerInterface customizer : customizers)
      {
         records = customizer.preInsert(insertInput, records, isPreview);
      }
      return records;
   }



   /***************************************************************************
    * run postInsert method over all sub-customizers
    ***************************************************************************/
   @Override
   public List<QRecord> postInsert(InsertInput insertInput, List<QRecord> records) throws QException
   {
      for(TableCustomizerInterface customizer : customizers)
      {
         records = customizer.postInsert(insertInput, records);
      }
      return records;
   }



   /***************************************************************************
    * run preUpdate method over all sub-customizers
    ***************************************************************************/
   @Override
   public List<QRecord> preUpdate(UpdateInput updateInput, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
   {
      for(TableCustomizerInterface customizer : customizers)
      {
         records = customizer.preUpdate(updateInput, records, isPreview, oldRecordList);
      }
      return records;
   }



   /***************************************************************************
    * run postUpdate method over all sub-customizers
    ***************************************************************************/
   @Override
   public List<QRecord> postUpdate(UpdateInput updateInput, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
   {
      for(TableCustomizerInterface customizer : customizers)
      {
         records = customizer.postUpdate(updateInput, records, oldRecordList);
      }
      return records;
   }



   /***************************************************************************
    * run preDelete method over all sub-customizers
    ***************************************************************************/
   @Override
   public List<QRecord> preDelete(DeleteInput deleteInput, List<QRecord> records, boolean isPreview) throws QException
   {
      for(TableCustomizerInterface customizer : customizers)
      {
         records = customizer.preDelete(deleteInput, records, isPreview);
      }
      return records;
   }



   /***************************************************************************
    * run postDelete method over all sub-customizers
    ***************************************************************************/
   @Override
   public List<QRecord> postDelete(DeleteInput deleteInput, List<QRecord> records) throws QException
   {
      for(TableCustomizerInterface customizer : customizers)
      {
         records = customizer.postDelete(deleteInput, records);
      }
      return records;
   }

}
