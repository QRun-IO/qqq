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

package com.kingsrook.qqq.backend.core.processes.implementations.general;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryRecordLink;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.utils.collections.AlphaNumericComparator;


/*******************************************************************************
 ** Helper class for process steps that want to roll up error summary and/or
 ** warning summary lines.  e.g., if the process might have a handful of different
 ** error messages.  Will record up to 50 (configurable) unique errors, then throw the rest int
 ** an "other" errors summary.
 *******************************************************************************/
public class ProcessSummaryWarningsAndErrorsRollup
{
   private Map<String, ProcessSummaryLine> errorSummaries = new HashMap<>();
   private Map<String, ProcessSummaryLine> warningSummaries = new HashMap<>();

   private ProcessSummaryLine otherErrorsSummary;
   private ProcessSummaryLine otherWarningsSummary;
   private ProcessSummaryLine errorTemplate;
   private ProcessSummaryLine warningTemplate;

   private int     uniqueErrorsToShow                         = 50;
   private boolean doReplaceSingletonCountLinesWithSuffixOnly = true;


   /*******************************************************************************
    **
    *******************************************************************************/
   public static ProcessSummaryWarningsAndErrorsRollup build(String pastTenseVerb)
   {
      return new ProcessSummaryWarningsAndErrorsRollup()
         .withErrorTemplate(new ProcessSummaryLine(Status.ERROR)
            .withSingularFutureMessage("record has an error: ")
            .withPluralFutureMessage("records have an error: ")
            .withSingularPastMessage("record had an error: ")
            .withPluralPastMessage("records had an error: "))
         .withWarningTemplate(new ProcessSummaryLine(Status.WARNING)
            .withSingularFutureMessage("record will be " + pastTenseVerb + ", but has a warning: ")
            .withPluralFutureMessage("records will be " + pastTenseVerb + ", but have a warning: ")
            .withSingularPastMessage("record was " + pastTenseVerb + ", but had a warning: ")
            .withPluralPastMessage("records were " + pastTenseVerb + ", but had a warning: "))
         .withOtherErrorsSummary(new ProcessSummaryLine(Status.ERROR)
            .withSingularFutureMessage("record has an other error.")
            .withPluralFutureMessage("records have other errors.")
            .withSingularPastMessage("record had an other error.")
            .withPluralPastMessage("records had other errors."))
         .withOtherWarningsSummary(new ProcessSummaryLine(Status.WARNING)
            .withSingularFutureMessage("record will be " + pastTenseVerb + ", but has an other warning.")
            .withPluralFutureMessage("records will be " + pastTenseVerb + ", but have other warnings.")
            .withSingularPastMessage("record was " + pastTenseVerb + ", but had other warnings.")
            .withPluralPastMessage("records were " + pastTenseVerb + ", but had other warnings."));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addToList(ArrayList<ProcessSummaryLineInterface> list)
   {
      addProcessSummaryLinesFromMap(list, errorSummaries);
      if(otherErrorsSummary != null)
      {
         otherErrorsSummary.addSelfToListIfAnyCount(list);
      }

      addProcessSummaryLinesFromMap(list, warningSummaries);
      if(otherWarningsSummary != null)
      {
         otherWarningsSummary.addSelfToListIfAnyCount(list);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addError(String message, Serializable primaryKey)
   {
      add(Status.ERROR, errorSummaries, errorTemplate, message, primaryKey);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addWarning(String message, Serializable primaryKey)
   {
      add(Status.WARNING, warningSummaries, warningTemplate, message, primaryKey);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public int countWarnings()
   {
      int sum = 0;
      for(ProcessSummaryLine processSummaryLine : warningSummaries.values())
      {
         sum += Objects.requireNonNullElse(processSummaryLine.getCount(), 0);
      }
      if(otherWarningsSummary != null)
      {
         sum += Objects.requireNonNullElse(otherWarningsSummary.getCount(), 0);
      }
      return (sum);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public int countErrors()
   {
      int sum = 0;
      for(ProcessSummaryLine processSummaryLine : errorSummaries.values())
      {
         sum += Objects.requireNonNullElse(processSummaryLine.getCount(), 0);
      }
      if(otherErrorsSummary != null)
      {
         sum += Objects.requireNonNullElse(otherErrorsSummary.getCount(), 0);
      }
      return (sum);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void add(Status status, Map<String, ProcessSummaryLine> summaryLineMap, ProcessSummaryLine templateLine, String message, Serializable primaryKey)
   {
      ProcessSummaryLine processSummaryLine = summaryLineMap.get(message);
      if(processSummaryLine == null)
      {
         if(summaryLineMap.size() < uniqueErrorsToShow)
         {
            processSummaryLine = new ProcessSummaryLine(status)
               .withMessageSuffix(message)
               .withSingularFutureMessage(templateLine.getSingularFutureMessage())
               .withPluralFutureMessage(templateLine.getPluralFutureMessage())
               .withSingularPastMessage(templateLine.getSingularPastMessage())
               .withPluralPastMessage(templateLine.getPluralPastMessage());
            summaryLineMap.put(message, processSummaryLine);
         }
         else
         {
            if(status.equals(Status.ERROR))
            {
               if(otherErrorsSummary == null)
               {
                  otherErrorsSummary = new ProcessSummaryLine(Status.ERROR).withMessageSuffix("records had an other error.");
               }
               processSummaryLine = otherErrorsSummary;
            }
            else
            {
               if(otherWarningsSummary == null)
               {
                  otherWarningsSummary = buildOtherWarningsSummary();
               }
               processSummaryLine = otherWarningsSummary;
            }
         }
      }

      if(primaryKey == null)
      {
         processSummaryLine.incrementCount();
      }
      else
      {
         processSummaryLine.incrementCountAndAddPrimaryKey(primaryKey);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static ProcessSummaryLine buildOtherWarningsSummary()
   {
      return new ProcessSummaryLine(Status.WARNING).withMessageSuffix("records had an other warning.");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void resetWarnings()
   {
      warningSummaries.clear();
      otherWarningsSummary = buildOtherWarningsSummary();
   }



   /*******************************************************************************
    ** Wrapper around AlphaNumericComparator for ProcessSummaryLineInterface that
    ** extracts string messages out.
    **
    ** Makes errors from bulk-insert look better when they report, e.g.
    ** Error parsing line #1: ...
    ** Error parsing line #2: ...
    ** Error parsing line #10: ...
    *******************************************************************************/
   private static class PSLAlphaNumericComparator implements Comparator<ProcessSummaryLineInterface>
   {
      private static AlphaNumericComparator alphaNumericComparator = new AlphaNumericComparator();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public int compare(ProcessSummaryLineInterface psli1, ProcessSummaryLineInterface psli2)
      {
         int messageComp = (alphaNumericComparator.compare(Objects.requireNonNullElse(psli1.getMessage(), ""), Objects.requireNonNullElse(psli2.getMessage(), "")));
         if(messageComp != 0)
         {
            return (messageComp);
         }

         if(psli1 instanceof ProcessSummaryLine psl1 && psli2 instanceof ProcessSummaryLine psl2)
         {
            return (alphaNumericComparator.compare(Objects.requireNonNullElse(psl1.getMessageSuffix(), ""), Objects.requireNonNullElse(psl2.getMessageSuffix(), "")));
         }

         return (0);
      }
   }



   /*******************************************************************************
    ** sort the process summary lines by count desc
    *******************************************************************************/
   private void addProcessSummaryLinesFromMap(ArrayList<ProcessSummaryLineInterface> rs, Map<String, ProcessSummaryLine> summaryMap)
   {
      summaryMap.values().stream()
         .sorted(Comparator.comparing((ProcessSummaryLine psl) -> Objects.requireNonNullElse(psl.getCount(), 0)).reversed()
            .thenComparing(new PSLAlphaNumericComparator())
         )
         .map(psl ->
         {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // this is to make lines that are like "1 record had an error:  Error parsing line #1:  blah" look better, by //
            // removing the redundant "1 record..." bit.                                                                  //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(doReplaceSingletonCountLinesWithSuffixOnly)
            {
               if(psl.getCount() == 1)
               {
                  return (new ProcessSummaryRecordLink().withStatus(psl.getStatus()).withLinkPreText(psl.getMessageSuffix()));
               }
            }

            return (psl);
         })
         .forEach(psli ->
         {
            if(psli instanceof ProcessSummaryLine psl)
            {
               psl.addSelfToListIfAnyCount(rs);
            }
            else
            {
               rs.add(psli);
            }
         });
   }



   /*******************************************************************************
    ** Getter for otherErrorsSummary
    *******************************************************************************/
   public ProcessSummaryLine getOtherErrorsSummary()
   {
      return (this.otherErrorsSummary);
   }



   /*******************************************************************************
    ** Setter for otherErrorsSummary
    *******************************************************************************/
   public void setOtherErrorsSummary(ProcessSummaryLine otherErrorsSummary)
   {
      this.otherErrorsSummary = otherErrorsSummary;
   }



   /*******************************************************************************
    ** Fluent setter for otherErrorsSummary
    *******************************************************************************/
   public ProcessSummaryWarningsAndErrorsRollup withOtherErrorsSummary(ProcessSummaryLine otherErrorsSummary)
   {
      this.otherErrorsSummary = otherErrorsSummary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for otherWarningsSummary
    *******************************************************************************/
   public ProcessSummaryLine getOtherWarningsSummary()
   {
      return (this.otherWarningsSummary);
   }



   /*******************************************************************************
    ** Setter for otherWarningsSummary
    *******************************************************************************/
   public void setOtherWarningsSummary(ProcessSummaryLine otherWarningsSummary)
   {
      this.otherWarningsSummary = otherWarningsSummary;
   }



   /*******************************************************************************
    ** Fluent setter for otherWarningsSummary
    *******************************************************************************/
   public ProcessSummaryWarningsAndErrorsRollup withOtherWarningsSummary(ProcessSummaryLine otherWarningsSummary)
   {
      this.otherWarningsSummary = otherWarningsSummary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for errorTemplate
    *******************************************************************************/
   public ProcessSummaryLine getErrorTemplate()
   {
      return (this.errorTemplate);
   }



   /*******************************************************************************
    ** Setter for errorTemplate
    *******************************************************************************/
   public void setErrorTemplate(ProcessSummaryLine errorTemplate)
   {
      this.errorTemplate = errorTemplate;
   }



   /*******************************************************************************
    ** Fluent setter for errorTemplate
    *******************************************************************************/
   public ProcessSummaryWarningsAndErrorsRollup withErrorTemplate(ProcessSummaryLine errorTemplate)
   {
      this.errorTemplate = errorTemplate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for warningTemplate
    *******************************************************************************/
   public ProcessSummaryLine getWarningTemplate()
   {
      return (this.warningTemplate);
   }



   /*******************************************************************************
    ** Setter for warningTemplate
    *******************************************************************************/
   public void setWarningTemplate(ProcessSummaryLine warningTemplate)
   {
      this.warningTemplate = warningTemplate;
   }



   /*******************************************************************************
    ** Fluent setter for warningTemplate
    *******************************************************************************/
   public ProcessSummaryWarningsAndErrorsRollup withWarningTemplate(ProcessSummaryLine warningTemplate)
   {
      this.warningTemplate = warningTemplate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for uniqueErrorsToShow
    *******************************************************************************/
   public int getUniqueErrorsToShow()
   {
      return (this.uniqueErrorsToShow);
   }



   /*******************************************************************************
    ** Setter for uniqueErrorsToShow
    *******************************************************************************/
   public void setUniqueErrorsToShow(int uniqueErrorsToShow)
   {
      this.uniqueErrorsToShow = uniqueErrorsToShow;
   }



   /*******************************************************************************
    ** Fluent setter for uniqueErrorsToShow
    *******************************************************************************/
   public ProcessSummaryWarningsAndErrorsRollup withUniqueErrorsToShow(int uniqueErrorsToShow)
   {
      this.uniqueErrorsToShow = uniqueErrorsToShow;
      return (this);
   }



   /*******************************************************************************
    ** Getter for doReplaceSingletonCountLinesWithSuffixOnly
    *******************************************************************************/
   public boolean getDoReplaceSingletonCountLinesWithSuffixOnly()
   {
      return (this.doReplaceSingletonCountLinesWithSuffixOnly);
   }



   /*******************************************************************************
    ** Setter for doReplaceSingletonCountLinesWithSuffixOnly
    *******************************************************************************/
   public void setDoReplaceSingletonCountLinesWithSuffixOnly(boolean doReplaceSingletonCountLinesWithSuffixOnly)
   {
      this.doReplaceSingletonCountLinesWithSuffixOnly = doReplaceSingletonCountLinesWithSuffixOnly;
   }



   /*******************************************************************************
    ** Fluent setter for doReplaceSingletonCountLinesWithSuffixOnly
    *******************************************************************************/
   public ProcessSummaryWarningsAndErrorsRollup withDoReplaceSingletonCountLinesWithSuffixOnly(boolean doReplaceSingletonCountLinesWithSuffixOnly)
   {
      this.doReplaceSingletonCountLinesWithSuffixOnly = doReplaceSingletonCountLinesWithSuffixOnly;
      return (this);
   }



   /*******************************************************************************
    ** Getter for errorSummaries
    **
    *******************************************************************************/
   public Map<String, ProcessSummaryLine> getErrorSummaries()
   {
      return errorSummaries;
   }
}
