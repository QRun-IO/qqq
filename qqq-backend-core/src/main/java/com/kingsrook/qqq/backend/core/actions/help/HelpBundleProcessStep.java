/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.help;


import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionOutputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Help table actions with standard review and result summary components.
 *******************************************************************************/
public class HelpBundleProcessStep implements BackendStep
{
   /*******************************************************************************
    ** Register table-scoped processes without adding navigation app children.
    *******************************************************************************/
   public static void defineAll(QInstance instance)
   {
      instance.addProcess(process("helpBundleSync", "Sync help from bundles", false));
      instance.addProcess(process("helpAcceptUpstream", "Accept upstream text", true));
      instance.addProcess(process("helpKeepMine", "Keep my text", true));
   }



   /*******************************************************************************
    ** Record actions require a selection; sync operates on the whole bundle set.
    *******************************************************************************/
   private static QProcessMetaData process(String name, String label, boolean selection)
   {
      return (new QProcessMetaData().withName(name).withLabel(label).withTableName("helpContent")
         .withMinInputRecords(selection ? 1 : 0)
         .withStep(new QBackendStepMetaData().withName("preview").withCode(new QCodeReference(HelpBundleProcessStep.class))
            .withOutputMetaData(new QFunctionOutputMetaData().withField(new QFieldMetaData(StreamedETLWithFrontendProcess.FIELD_VALIDATION_SUMMARY, QFieldType.STRING))))
         .withStep(new QFrontendStepMetaData().withName("review")
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.VALIDATION_REVIEW_SCREEN)))
         .withStep(new QBackendStepMetaData().withName("execute").withCode(new QCodeReference(HelpBundleProcessStep.class))
            .withOutputMetaData(new QFunctionOutputMetaData().withField(new QFieldMetaData(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY, QFieldType.STRING))))
         .withStep(new QFrontendStepMetaData().withName("result")
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.PROCESS_SUMMARY_RESULTS))));
   }



   /*******************************************************************************
    ** Preview never writes; execution recalculates against current database text.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      boolean preview = "preview".equals(input.getStepName());
      HelpBundleSyncAction action = new HelpBundleSyncAction();
      ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
      if("helpBundleSync".equals(input.getProcessName()))
      {
         HelpBundleSyncAction.Result result = preview ? action.preview() : action.execute();
         add(lines, result.inserted(), "inserted", preview);
         add(lines, result.updated(), "updated", preview);
         add(lines, result.upstreamChanged(), "flagged", preview);
         add(lines, result.retired(), "retired", preview);
         add(lines, result.deleted(), "deleted", preview);
         add(lines, result.unchanged(), "left unchanged", preview);
      }
      else
      {
         List<Serializable> ids = selectionIds(input);
         if(!preview)
         {
            if("helpAcceptUpstream".equals(input.getProcessName()))
            {
               action.acceptUpstream(ids);
            }
            else
            {
               action.keepMine(ids);
            }
         }
         add(lines, ids.size(), "resolved", preview);
      }
      output.addValue(preview ? StreamedETLWithFrontendProcess.FIELD_VALIDATION_SUMMARY : StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY, lines);
   }



   /*******************************************************************************
    ** Consistent future/past tense summaries, including zero counts.
    *******************************************************************************/
   private void add(ArrayList<ProcessSummaryLineInterface> lines, int count, String verb, boolean preview)
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.INFO)
         .withSingularFutureMessage("entry would be " + verb)
         .withPluralFutureMessage("entries would be " + verb)
         .withSingularPastMessage("entry was " + verb)
         .withPluralPastMessage("entries were " + verb);
      line.setCount(count);
      line.prepareForFrontend(!preview);
      lines.add(line);
   }



   /*******************************************************************************
    ** Support explicit selections and the normal select-all query-filter contract.
    *******************************************************************************/
   private List<Serializable> selectionIds(RunBackendStepInput input) throws QException
   {
      QQueryFilter filter = null;
      String json = input.getValueString("queryFilterJson");
      if(!StringUtils.hasContent(json))
      {
         json = input.getValueString("filterJSON");
      }
      if(StringUtils.hasContent(json))
      {
         try
         {
            filter = JsonUtils.toObject(json, QQueryFilter.class);
         }
         catch(IOException e)
         {
            throw new QException("Invalid help selection filter", e);
         }
      }
      else if(input.getCallback() != null && input.getCallback().getQueryFilter() != null)
      {
         filter = input.getCallback().getQueryFilter();
      }
      else
      {
         List<Serializable> ids = new ArrayList<>();
         if(input.getRecords() != null)
         {
            input.getRecords().forEach(record -> ids.add(record.getValue("id")));
         }
         if(StringUtils.hasContent(input.getValueString("recordIds")))
         {
            ids.addAll(List.of(input.getValueString("recordIds").split(",")));
         }
         if(ids.isEmpty())
         {
            throw new QException("Select help entries awaiting upstream review.");
         }
         filter = new QQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.IN, ids));
      }
      if(filter == null)
      {
         throw new QException("Select help entries awaiting upstream review.");
      }
      List<Serializable> ids = new ArrayList<>();
      new QueryAction().execute(new QueryInput("helpContent").withFilter(filter)).getRecords().stream()
         .filter(record -> "UPSTREAM_CHANGED".equals(record.getValueString("upstreamStatus")))
         .forEach(record -> ids.add(record.getValue("id")));
      return (ids);
   }
}
