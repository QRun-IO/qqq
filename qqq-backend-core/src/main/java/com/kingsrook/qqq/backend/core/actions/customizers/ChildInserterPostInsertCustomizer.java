/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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
import java.util.Iterator;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.QStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Standard/re-usable post-insert customizer, for the use case where, when we
 ** do an insert into table "parent", we want a record automatically inserted into
 ** table "child".  Optionally (based on RelationshipType), there can be a foreign
 ** key in "parent", pointed at "child".  e.g., named: "parent.childId".
 **
 *******************************************************************************/
public abstract class ChildInserterPostInsertCustomizer extends AbstractPostInsertCustomizer
{
   /***************************************************************************
    **
    ***************************************************************************/
   public enum RelationshipType
   {
      PARENT_POINTS_AT_CHILD,
      CHILD_POINTS_AT_PARENT
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public abstract QRecord buildChildForRecord(QRecord parentRecord) throws QException;

   /*******************************************************************************
    **
    *******************************************************************************/
   public abstract String getChildTableName();



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getForeignKeyFieldName()
   {
      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public abstract RelationshipType getRelationshipType();



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> apply(List<QRecord> records)
   {
      try
      {
         List<QRecord>  rs               = records;
         List<QRecord>  childrenToInsert = new ArrayList<>();
         QTableMetaData table            = getInsertInput().getTable();
         QTableMetaData childTable       = QContext.getQInstance().getTable(getChildTableName());

         ////////////////////////////////////////////////////////////////////////////////
         // iterate over the inserted records, building a list child records to insert //
         // for ones missing a value in the foreign key field.                         //
         ////////////////////////////////////////////////////////////////////////////////
         switch(getRelationshipType())
         {
            case PARENT_POINTS_AT_CHILD ->
            {
               String foreignKeyFieldName = getForeignKeyFieldName();
               try
               {
                  table.getField(foreignKeyFieldName);
               }
               catch(Exception e)
               {
                  throw new QRuntimeException("For RelationshipType.PARENT_POINTS_AT_CHILD, a valid foreignKeyFieldName in the parent table must be given.  "
                     + "[" + foreignKeyFieldName + "] is not a valid field name in table [" + table.getName() + "]");
               }

               for(QRecord record : records)
               {
                  if(record.getValue(foreignKeyFieldName) == null)
                  {
                     childrenToInsert.add(buildChildForRecord(record));
                  }
               }
            }
            case CHILD_POINTS_AT_PARENT ->
            {
               for(QRecord record : records)
               {
                  childrenToInsert.add(buildChildForRecord(record));
               }
            }
            default -> throw new IllegalStateException("Unexpected value: " + getRelationshipType());
         }

         ///////////////////////////////////////////////////////////////////////////////////
         // if there are no children to insert, then just return the original record list //
         ///////////////////////////////////////////////////////////////////////////////////
         if(childrenToInsert.isEmpty())
         {
            return (records);
         }

         /////////////////////////
         // insert the children //
         /////////////////////////
         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(getChildTableName());
         insertInput.setRecords(childrenToInsert);
         insertInput.setTransaction(this.insertInput.getTransaction());
         InsertOutput      insertOutput           = new InsertAction().execute(insertInput);
         Iterator<QRecord> insertedRecordIterator = insertOutput.getRecords().iterator();

         /////////////////////////////////////////////////////////////////////////////////
         // check for any errors when inserting the children, if any errors were found, //
         // then set a warning in the parent with the details of the problem            //
         /////////////////////////////////////////////////////////////////////////////////

         //////////////////////////////////////////////////////////////////////////////////////////////////////
         // for the PARENT_POINTS_AT_CHILD relationship type:
         // iterate over the original list of records again - for any that need a child (e.g., are missing   //
         // foreign key), set their foreign key to a newly inserted child's key, and add them to be updated. //
         //////////////////////////////////////////////////////////////////////////////////////////////////////
         switch(getRelationshipType())
         {
            case PARENT_POINTS_AT_CHILD ->
            {
               rs = new ArrayList<>();
               List<QRecord> recordsToUpdate = new ArrayList<>();
               for(QRecord record : records)
               {
                  Serializable primaryKey = record.getValue(table.getPrimaryKeyField());
                  if(record.getValue(getForeignKeyFieldName()) == null)
                  {
                     ///////////////////////////////////////////////////////////////////////////////////////////////////
                     // get the corresponding child record, if it has any errors, set that as a warning in the parent //
                     ///////////////////////////////////////////////////////////////////////////////////////////////////
                     QRecord childRecord = insertedRecordIterator.next();
                     if(CollectionUtils.nullSafeHasContents(childRecord.getErrors()))
                     {
                        for(QStatusMessage error : childRecord.getErrors())
                        {
                           record.addWarning(new QWarningMessage("Error creating child " + childTable.getLabel() + " (" + error.toString() + ")"));
                        }
                        rs.add(record);
                        continue;
                     }

                     Serializable foreignKey = childRecord.getValue(childTable.getPrimaryKeyField());
                     recordsToUpdate.add(new QRecord().withValue(table.getPrimaryKeyField(), primaryKey).withValue(getForeignKeyFieldName(), foreignKey));
                     record.setValue(getForeignKeyFieldName(), foreignKey);
                     rs.add(record);
                  }
                  else
                  {
                     rs.add(record);
                  }
               }

               ////////////////////////////////////////////////////////////////////////////
               // update the originally inserted records to reference their new children //
               ////////////////////////////////////////////////////////////////////////////
               UpdateInput updateInput = new UpdateInput();
               updateInput.setTableName(getInsertInput().getTableName());
               updateInput.setRecords(recordsToUpdate);
               updateInput.setTransaction(this.insertInput.getTransaction());
               new UpdateAction().execute(updateInput);
            }
            case CHILD_POINTS_AT_PARENT ->
            {
               ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // todo - some version of looking at the inserted children to confirm that they were inserted, and updating the parents with warnings if they weren't //
               ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            }
            default -> throw new IllegalStateException("Unexpected value: " + getRelationshipType());
         }

         return (rs);
      }
      catch(RuntimeException re)
      {
         throw (re);
      }
      catch(Exception e)
      {
         throw new RuntimeException("Error inserting new child records for new parent records", e);
      }
   }
}
