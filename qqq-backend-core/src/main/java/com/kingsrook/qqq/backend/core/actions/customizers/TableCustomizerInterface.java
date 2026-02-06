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

package com.kingsrook.qqq.backend.core.actions.customizers;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Common interface used by all (core) TableCustomizer types (e.g., post-query,
 ** and {pre,post}-{insert,update,delete}.
 **
 ** Note that the abstract-base classes for each action still exist, though have
 ** been back-ported to be implementors of this interface.  The action classes
 ** will now expect this type, and call this type's methods.
 **
 *******************************************************************************/
public interface TableCustomizerInterface
{
   QLogger LOG = QLogger.getLogger(TableCustomizerInterface.class);


   /*******************************************************************************
    ** custom actions to run after table meta data was created by the meta data action
    **
    *******************************************************************************/
   default void postMetaData(TableMetaDataInput tableMetaDataInput, TableMetaDataOutput tableMetaDataOutput) throws QException
   {
      LOG.info("A default implementation of postMetaData is running...  Probably not expected!", logPair("tableName", tableMetaDataInput.getTableName()));
   }


   /*******************************************************************************
    ** custom actions to run after a query (or get!) takes place.
    **
    *******************************************************************************/
   default List<QRecord> postQuery(QueryOrGetInputInterface queryInput, List<QRecord> records) throws QException
   {
      LOG.info("A default implementation of postQuery is running...  Probably not expected!", logPair("tableName", queryInput.getTableName()));
      return (records);
   }


   /*******************************************************************************
    ** custom actions before an insert takes place.
    **
    ** It's important for implementations to be aware of the isPreview field, which
    ** is set to true when the code is running to give users advice, e.g., on a review
    ** screen - vs. being false when the action is ACTUALLY happening.  So, if you're doing
    ** things like storing data, you don't want to do that if isPreview is true!!
    **
    ** General implementation would be, to iterate over the records (the inputs to
    ** the insert action), and look at their values:
    ** - possibly adding Errors (`addError`) or Warnings (`addWarning`) to the records
    ** - possibly manipulating values (`setValue`)
    ** - possibly throwing an exception - if you really don't want the insert operation to continue.
    ** - doing "whatever else" you may want to do.
    ** - returning the list of records (can be the input list) that you want to go on to the backend implementation class.
    *******************************************************************************/
   default List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
   {
      try
      {
         return (preInsertOrUpdate(insertInput, records, isPreview, Optional.empty()));
      }
      catch(NotImplementedHereException e)
      {
         LOG.info("A default implementation of preInsert is running...  Probably not expected!", logPair("tableName", insertInput.getTableName()));
         return (records);
      }
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   default AbstractPreInsertCustomizer.WhenToRun whenToRunPreInsert(InsertInput insertInput, boolean isPreview)
   {
      return (AbstractPreInsertCustomizer.WhenToRun.AFTER_ALL_VALIDATIONS);
   }


   /*******************************************************************************
    ** custom actions after an insert takes place.
    **
    ** General implementation would be, to iterate over the records (the outputs of
    ** the insert action), and look at their values:
    ** - possibly adding Errors (`addError`) or Warnings (`addWarning`) to the records
    ** - possibly throwing an exception - though doing so won't stop the update, and instead
    **   will just set a warning on all of the updated records...
    ** - doing "whatever else" you may want to do.
    ** - returning the list of records (can be the input list) that you want to go back to the caller.
    *******************************************************************************/
   default List<QRecord> postInsert(InsertInput insertInput, List<QRecord> records) throws QException
   {
      try
      {
         return (postInsertOrUpdate(insertInput, records, Optional.empty()));
      }
      catch(NotImplementedHereException e)
      {
         LOG.info("A default implementation of postInsert is running...  Probably not expected!", logPair("tableName", insertInput.getTableName()));
         return (records);
      }
   }


   /*******************************************************************************
    ** custom actions before an update takes place.
    **
    ** It's important for implementations to be aware of the isPreview field, which
    ** is set to true when the code is running to give users advice, e.g., on a review
    ** screen - vs. being false when the action is ACTUALLY happening.  So, if you're doing
    ** things like storing data, you don't want to do that if isPreview is true!!
    **
    ** General implementation would be, to iterate over the records (the inputs to
    ** the update action), and look at their values:
    ** - possibly adding Errors (`addError`) or Warnings (`addWarning`) to the records
    ** - possibly manipulating values (`setValue`)
    ** - possibly throwing an exception - if you really don't want the update operation to continue.
    ** - doing "whatever else" you may want to do.
    ** - returning the list of records (can be the input list) that you want to go on to the backend implementation class.
    **
    ** Note, "old records" (e.g., with values freshly fetched from the backend) will be
    ** available (if the backend supports it)
    *******************************************************************************/
   default List<QRecord> preUpdate(UpdateInput updateInput, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
   {
      try
      {
         return (preInsertOrUpdate(updateInput, records, isPreview, oldRecordList));
      }
      catch(NotImplementedHereException e)
      {
         LOG.info("A default implementation of preUpdate is running...  Probably not expected!", logPair("tableName", updateInput.getTableName()));
         return (records);
      }
   }


   /*******************************************************************************
    ** custom actions after an update takes place.
    **
    ** General implementation would be, to iterate over the records (the outputs of
    ** the update action), and look at their values:
    ** - possibly adding Errors (`addError`) or Warnings (`addWarning`) to the records?
    ** - possibly throwing an exception - though doing so won't stop the update, and instead
    **   will just set a warning on all of the updated records...
    ** - doing "whatever else" you may want to do.
    ** - returning the list of records (can be the input list) that you want to go back to the caller.
    **
    ** Note, "old records" (e.g., with values freshly fetched from the backend) will be
    ** available (if the backend supports it).
    *******************************************************************************/
   default List<QRecord> postUpdate(UpdateInput updateInput, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
   {
      try
      {
         return (postInsertOrUpdate(updateInput, records, oldRecordList));
      }
      catch(NotImplementedHereException e)
      {
         LOG.info("A default implementation of postUpdate is running...  Probably not expected!", logPair("tableName", updateInput.getTableName()));
         return (records);
      }
   }


   /*******************************************************************************
    ** Custom actions before a delete takes place.
    **
    ** It's important for implementations to be aware of the isPreview param, which
    ** is set to true when the code is running to give users advice, e.g., on a review
    ** screen - vs. being false when the action is ACTUALLY happening.  So, if you're doing
    ** things like storing data, you don't want to do that if isPreview is true!!
    **
    ** General implementation would be, to iterate over the records (which the DeleteAction
    ** would look up based on the inputs to the delete action), and look at their values:
    ** - possibly adding Errors (`addError`) or Warnings (`addWarning`) to the records
    ** - possibly throwing an exception - if you really don't want the delete operation to continue.
    ** - doing "whatever else" you may want to do.
    ** - returning the list of records (can be the input list) - this is how errors
    **   and warnings are propagated to the DeleteAction.  Note that any records with
    **   an error will NOT proceed to the backend's delete interface - but those with
    **   warnings will.
    *******************************************************************************/
   default List<QRecord> preDelete(DeleteInput deleteInput, List<QRecord> records, boolean isPreview) throws QException
   {
      LOG.info("A default implementation of preDelete is running...  Probably not expected!", logPair("tableName", deleteInput.getTableName()));
      return (records);
   }


   /*******************************************************************************
    ** Custom actions after a delete takes place.
    **
    ** General implementation would be, to iterate over the records (ones which didn't
    ** have a delete error), and look at their values:
    ** - possibly adding Errors (`addError`) or Warnings (`addWarning`) to the records?
    ** - possibly throwing an exception - though doing so won't stop the delete, and instead
    **   will just set a warning on all of the deleted records...
    ** - doing "whatever else" you may want to do.
    ** - returning the list of records (can be the input list) that you want to go back
    **   to the caller - this is how errors and warnings are propagated .
    *******************************************************************************/
   default List<QRecord> postDelete(DeleteInput deleteInput, List<QRecord> records) throws QException
   {
      LOG.info("A default implementation of postDelete is running...  Probably not expected!", logPair("tableName", deleteInput.getTableName()));
      return (records);
   }


   /***************************************************************************
    ** Optional method to override in a customizer that does the same thing for
    ** both preInsert & preUpdate.
    ***************************************************************************/
   default List<QRecord> preInsertOrUpdate(AbstractActionInput input, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
   {
      throw NotImplementedHereException.instance;
   }


   /***************************************************************************
    ** Optional method to override in a customizer that does the same thing for
    ** both postInsert & postUpdate.
    ***************************************************************************/
   default List<QRecord> postInsertOrUpdate(AbstractActionInput input, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
   {
      throw NotImplementedHereException.instance;
   }


   /***************************************************************************
    **
    ***************************************************************************/
   default Optional<Map<Serializable, QRecord>> oldRecordListToMap(String primaryKeyField, Optional<List<QRecord>> oldRecordList)
   {
      if(oldRecordList.isPresent())
      {
         return (Optional.of(CollectionUtils.listToMap(oldRecordList.get(), r -> r.getValue(primaryKeyField))));
      }
      else
      {
         return (Optional.empty());
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   class NotImplementedHereException extends QException
   {
      private static NotImplementedHereException instance = new NotImplementedHereException();



      /***************************************************************************
       **
       ***************************************************************************/
      private NotImplementedHereException()
      {
         super("Not implemented here");
      }
   }
}
