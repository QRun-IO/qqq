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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.edit;


import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobCallback;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.actions.async.AsyncRecordPipeLoop;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeSupplier;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep.FIELD_SOURCE_TABLE;


/*******************************************************************************
 ** class for preparing data for the bulk edit processes
 *******************************************************************************/
public class PrepareBulkEditStep implements BackendStep
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      //////////////////////////////////////////////
      // get table metadata and find any possible //
      // value sources that have a PVS filter     //
      //////////////////////////////////////////////
      QTableMetaData table = QContext.getQInstance().getTable(runBackendStepInput.getTableName());
      runBackendStepInput.addValue(FIELD_SOURCE_TABLE, table.getName());

      ///////////////////////////////////////////////////////////////////////////////////
      // create a listing hash of the PVS dependency field names to the original field //
      ///////////////////////////////////////////////////////////////////////////////////
      ListingHash<String, String> dependencyFieldToPVSFieldMap = new ListingHash<>();
      for(String fieldName : table.getFields().keySet())
      {
         QFieldMetaData field  = table.getField(fieldName);
         QQueryFilter   filter = field.getPossibleValueSourceFilter();

         /////////////////////////////////////////////////////////////
         // if the field is not editable, no need to worry about it //
         /////////////////////////////////////////////////////////////
         if(!BooleanUtils.isTrue(field.getIsEditable()))
         {
            continue;
         }

         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // if there is a single filter, with a single criteria matching 'input.', parse the pvs field name //
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         if(filter != null && filter.hasAnyCriteria() && filter.getCriteria().size() == 1)
         {
            List<Serializable> value = filter.getCriteria().get(0).getValues();
            if(value.size() == 1 && value.toString().contains("input."))
            {
               Pattern pattern = Pattern.compile("\\$\\{input\\.([a-zA-Z_][a-zA-Z0-9_]*)}");
               Matcher matcher = pattern.matcher(value.toString());
               if(matcher.find())
               {
                  String         dependentFieldName = matcher.group(1);
                  QFieldMetaData dependentField     = table.getFields().get(dependentFieldName);

                  //////////////////////////////////////////////////////////////
                  // if the dependent field isn't editable, ignore it as well //
                  //////////////////////////////////////////////////////////////
                  if(dependentField == null || !BooleanUtils.isTrue(dependentField.getIsEditable()))
                  {
                     continue;
                  }
                  dependencyFieldToPVSFieldMap.add(dependentFieldName, fieldName);
               }
            }
         }
      }

      //////////////////////////////////////////////////////////////////
      // if recordIds are available, use them to look up the entities //
      // run through a pipe for the case of thousands of records      //
      //////////////////////////////////////////////////////////////////
      ListingHash<String, String> nonDistinctPVSFields = new ListingHash<>();

      RecordPipe          pipe                = new RecordPipe();
      String              jobUUID             = UUID.randomUUID().toString();
      AsyncRecordPipeLoop asyncRecordPipeLoop = new AsyncRecordPipeLoop().withForcedJobUUID(jobUUID);

      /////////////////////////////////////////////////////////////////////
      // set up supplier, to do a query, feeding its records into a pipe //
      /////////////////////////////////////////////////////////////////////
      UnsafeFunction<AsyncJobCallback, Serializable, QException> supplier = (callback) ->
      {
         /////////////////////////////////////////////////////////////////////////
         // use an extract via query step to get proper query filter from input //
         /////////////////////////////////////////////////////////////////////////
         QQueryFilter queryFilter = new QueryFilterExtractor().extractQueryFilter(runBackendStepInput);
         QueryInput   queryInput  = new QueryInput(table.getName()).withFilter(queryFilter);
         queryInput.setRecordPipe(pipe);
         new QueryAction().execute(queryInput);
         return (true);
      };

      //////////////////////////////////////////////////////////////////
      // consume records from the pipe, and build up a dependency map //
      //////////////////////////////////////////////////////////////////
      Map<String, Set<Serializable>> valuesMap = new HashMap<>();
      UnsafeSupplier<Integer, QException> consumer = () ->
      {
         List<QRecord> availableRecords = pipe.consumeAvailableRecords();

         ////////////////////////////////////////////////////////////////////////////////////////////////////
         // if all values in the valuesMap are greater than one in size, we can stop iterating in the pipe //
         ////////////////////////////////////////////////////////////////////////////////////////////////////
         for(String pvsFilterFieldName : valuesMap.keySet())
         {
            ///////////////////////////////////////////////////////////
            // if any still one or less, we still need to keep going //
            ///////////////////////////////////////////////////////////
            if(valuesMap.get(pvsFilterFieldName).size() <= 1)
            {
               break;
            }

            /////////////////////////////////////
            // otherwise, tell the job to stop //
            /////////////////////////////////////
            new AsyncJobManager().cancelJob(jobUUID);
         }

         ////////////////////////////////////////////////////////////////////////////////////////////////////////
         // now for each possible value field we found, look up the values from the input records, if values   //
         // are found, and they are all the same value, then we can use the PVS for that field, otherwise, not //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////
         for(String pvsFilterFieldName : dependencyFieldToPVSFieldMap.keySet())
         {
            for(QRecord qRecord : availableRecords)
            {
               if(qRecord.getValues().containsKey(pvsFilterFieldName))
               {
                  Set<Serializable> values = valuesMap.getOrDefault(pvsFilterFieldName, new HashSet<>());
                  values.add(qRecord.getValue(pvsFilterFieldName));
                  valuesMap.put(pvsFilterFieldName, values);
               }

               ////////////////////////////////////////////////////////
               // if more than one distinct value, break out of loop //
               ////////////////////////////////////////////////////////
               if(valuesMap.get(pvsFilterFieldName).size() > 1)
               {
                  dependencyFieldToPVSFieldMap.get(pvsFilterFieldName).forEach(f -> nonDistinctPVSFields.add(table.getField(pvsFilterFieldName).getLabel(), table.getField(f).getLabel()));
                  break;
               }
            }
         }

         return (availableRecords.size());
      };

      //////////////////////////////////////////////////
      // run a pipe loop for our query + manipulation //
      //////////////////////////////////////////////////
      asyncRecordPipeLoop.run(getClass().getSimpleName(), null, pipe, supplier, consumer);

      for(String pvsFilterFieldName : dependencyFieldToPVSFieldMap.keySet())
      {
         //////////////////////////////////////////////////////////////////////////////////
         // if a single unique value was found, put that as a value in the output object //
         //////////////////////////////////////////////////////////////////////////////////
         if(valuesMap.containsKey(pvsFilterFieldName) && valuesMap.get(pvsFilterFieldName).size() == 1)
         {
            Serializable pvsFilterValue = valuesMap.get(pvsFilterFieldName).iterator().next();
            runBackendStepOutput.addValue("possibleValueFilterValue" + StringUtils.ucFirst(pvsFilterFieldName), pvsFilterValue);
         }
      }

      if(CollectionUtils.nullSafeHasContents(nonDistinctPVSFields))
      {
         runBackendStepOutput.addValue("nonDistinctPVSFields", nonDistinctPVSFields);
      }
   }



   /***************************************************************************
    * we need to call ExtractViaQueryStep.getQueryFilter, but, it is protected...
    * rather than change the class hierarchy, let's just expose it for ourselves
    * and if others need this in the future, consider other options?
    ***************************************************************************/
   private static class QueryFilterExtractor extends ExtractViaQueryStep
   {
      /***************************************************************************
       * call getQueryFilter from the base class, returning its output.
       ***************************************************************************/
      public QQueryFilter extractQueryFilter(RunBackendStepInput runBackendStepInput) throws QException
      {
         return getQueryFilter(runBackendStepInput);
      }
   }
}
