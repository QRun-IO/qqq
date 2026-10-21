/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist;


import java.util.List;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;


/*******************************************************************************
 ** The results of the assisted bulk-load operations ({@link BulkLoadAssist}).
 *******************************************************************************/
public final class BulkLoadAssistModels
{
   /*******************************************************************************
    ** Utility holder.
    *******************************************************************************/
   private BulkLoadAssistModels()
   {
   }



   /*******************************************************************************
    ** One column of an upload: its shape, and sample values only where allowed.
    *******************************************************************************/
   public record ColumnProfile(int columnIndex, String columnLetter, String header, String inferredType, double percentBlank, int distinctCount,
                               boolean distinctCountCapped, int maxLength, String patternClass, String mappedFieldName, String classification,
                               boolean samplesAllowed, List<String> sampleValues)
   {
   }



   /*******************************************************************************
    ** An upload's header guess and column profiles.
    *******************************************************************************/
   public record UploadProfile(String uploadRef, String fileName, boolean hasHeaderRow, int rowsProfiled, List<ColumnProfile> columns)
   {
   }



   /*******************************************************************************
    ** A file column (index and header).
    *******************************************************************************/
   public record ColumnRef(int columnIndex, String header)
   {
   }



   /*******************************************************************************
    ** A table field (name, label, possible value source if any).
    *******************************************************************************/
   public record FieldRef(String fieldName, String label, String possibleValueSourceName)
   {
   }



   /*******************************************************************************
    ** A suggested mapping, with what it leaves unmapped and what needs value
    ** mapping.
    *******************************************************************************/
   public record Suggestion(BulkLoadProfile profile, List<ColumnRef> unmappedColumns, List<FieldRef> unmappedRequiredFields, List<FieldRef> valueMappingFields)
   {
   }



   /*******************************************************************************
    ** One line of the standard process summary.
    *******************************************************************************/
   public record SummaryLine(String status, Integer count, String message, List<String> bulletsOfText)
   {
   }



   /*******************************************************************************
    ** An error group: the file rows, the field when known, the error type and a
    ** message (masked for classified fields on a STANDARD route).
    *******************************************************************************/
   public record RowError(String rowNumbers, String fieldName, String errorCode, String message)
   {
   }



   /*******************************************************************************
    ** A dry run's result. Nothing is stored. jobId is the seam for a FULL run
    ** over the durable threshold becoming a VALIDATE job (spec #1171 3.2);
    ** always null here.
    *******************************************************************************/
   public record Validation(String mode, String processName, Integer recordCount, int okCount, int errorCount, List<SummaryLine> summaryLines,
                            List<RowError> errors, Integer previewRecordCount, Long jobId)
   {
   }
}
