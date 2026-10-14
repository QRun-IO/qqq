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


package com.kingsrook.qqq.backend.core.jobs.model;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;


/*******************************************************************************
 ** A request to run a process as a durable job (DurableJobs.launch, and the
 ** body of POST /qqq/v1/processes/{processName}/jobs).
 *******************************************************************************/
public class LaunchRequest
{
   private String                    processName;
   private Map<String, Serializable> values         = new LinkedHashMap<>();
   private QQueryFilter              filter;
   private List<Serializable>        recordIds;
   private List<String>              uploadRefs     = new ArrayList<>();
   private Integer                   savedBulkLoadProfileId;
   private String                    profileJson;
   private JobMode                   mode           = JobMode.EXECUTE;
   private Integer                   priority;
   private Boolean                   notifyEmail    = false;
   private String                    label;
   private String                    startAfterStep;
   private Integer                   parentJobId;
   private Long                      expectedRows;
   private Map<String, Object>       rowSetSource;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public LaunchRequest()
   {
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public LaunchRequest(String processName)
   {
      this.processName = processName;
   }



   /*******************************************************************************
    ** Getter for processName
    *******************************************************************************/
   public String getProcessName()
   {
      return (this.processName);
   }



   /*******************************************************************************
    ** Fluent setter for processName
    *******************************************************************************/
   public LaunchRequest withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for values
    *******************************************************************************/
   public Map<String, Serializable> getValues()
   {
      return (this.values);
   }



   /*******************************************************************************
    ** Fluent setter for values
    *******************************************************************************/
   public LaunchRequest withValues(Map<String, Serializable> values)
   {
      this.values = values == null ? new LinkedHashMap<>() : values;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for one value
    *******************************************************************************/
   public LaunchRequest withValue(String name, Serializable value)
   {
      this.values.put(name, value);
      return (this);
   }



   /*******************************************************************************
    ** Getter for filter
    *******************************************************************************/
   public QQueryFilter getFilter()
   {
      return (this.filter);
   }



   /*******************************************************************************
    ** Fluent setter for filter
    *******************************************************************************/
   public LaunchRequest withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordIds
    *******************************************************************************/
   public List<Serializable> getRecordIds()
   {
      return (this.recordIds);
   }



   /*******************************************************************************
    ** Fluent setter for recordIds
    *******************************************************************************/
   public LaunchRequest withRecordIds(List<Serializable> recordIds)
   {
      this.recordIds = recordIds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for uploadRefs
    *******************************************************************************/
   public List<String> getUploadRefs()
   {
      return (this.uploadRefs);
   }



   /*******************************************************************************
    ** Fluent setter for uploadRefs
    *******************************************************************************/
   public LaunchRequest withUploadRefs(List<String> uploadRefs)
   {
      this.uploadRefs = uploadRefs == null ? new ArrayList<>() : uploadRefs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for savedBulkLoadProfileId
    *******************************************************************************/
   public Integer getSavedBulkLoadProfileId()
   {
      return (this.savedBulkLoadProfileId);
   }



   /*******************************************************************************
    ** Fluent setter for savedBulkLoadProfileId
    *******************************************************************************/
   public LaunchRequest withSavedBulkLoadProfileId(Integer savedBulkLoadProfileId)
   {
      this.savedBulkLoadProfileId = savedBulkLoadProfileId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for profileJson
    *******************************************************************************/
   public String getProfileJson()
   {
      return (this.profileJson);
   }



   /*******************************************************************************
    ** Fluent setter for profileJson
    *******************************************************************************/
   public LaunchRequest withProfileJson(String profileJson)
   {
      this.profileJson = profileJson;
      return (this);
   }



   /*******************************************************************************
    ** Getter for mode
    *******************************************************************************/
   public JobMode getMode()
   {
      return (this.mode);
   }



   /*******************************************************************************
    ** Fluent setter for mode
    *******************************************************************************/
   public LaunchRequest withMode(JobMode mode)
   {
      this.mode = mode == null ? JobMode.EXECUTE : mode;
      return (this);
   }



   /*******************************************************************************
    ** Getter for priority
    *******************************************************************************/
   public Integer getPriority()
   {
      return (this.priority);
   }



   /*******************************************************************************
    ** Fluent setter for priority
    *******************************************************************************/
   public LaunchRequest withPriority(Integer priority)
   {
      this.priority = priority;
      return (this);
   }



   /*******************************************************************************
    ** Getter for notifyEmail
    *******************************************************************************/
   public Boolean getNotifyEmail()
   {
      return (this.notifyEmail);
   }



   /*******************************************************************************
    ** Fluent setter for notifyEmail
    *******************************************************************************/
   public LaunchRequest withNotifyEmail(Boolean notifyEmail)
   {
      this.notifyEmail = notifyEmail;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public LaunchRequest withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for startAfterStep: the launch point (frontend values collected up to
    ** here); backend steps after it run in the job.
    *******************************************************************************/
   public String getStartAfterStep()
   {
      return (this.startAfterStep);
   }



   /*******************************************************************************
    ** Fluent setter for startAfterStep
    *******************************************************************************/
   public LaunchRequest withStartAfterStep(String startAfterStep)
   {
      this.startAfterStep = startAfterStep;
      return (this);
   }



   /*******************************************************************************
    ** Getter for parentJobId (retry or run-again of)
    *******************************************************************************/
   public Integer getParentJobId()
   {
      return (this.parentJobId);
   }



   /*******************************************************************************
    ** Fluent setter for parentJobId
    *******************************************************************************/
   public LaunchRequest withParentJobId(Integer parentJobId)
   {
      this.parentJobId = parentJobId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for expectedRows (a known row count, e.g., from count_records)
    *******************************************************************************/
   public Long getExpectedRows()
   {
      return (this.expectedRows);
   }



   /*******************************************************************************
    ** Fluent setter for expectedRows
    *******************************************************************************/
   public LaunchRequest withExpectedRows(Long expectedRows)
   {
      this.expectedRows = expectedRows;
      return (this);
   }




   /*******************************************************************************
    ** Getter for rowSetSource: for "retry failed rows", the parent job and the
    ** rows (row numbers or record keys) to run again.
    *******************************************************************************/
   public Map<String, Object> getRowSetSource()
   {
      return (this.rowSetSource);
   }



   /*******************************************************************************
    ** Fluent setter for rowSetSource
    *******************************************************************************/
   public LaunchRequest withRowSetSource(Map<String, Object> rowSetSource)
   {
      this.rowSetSource = rowSetSource;
      return (this);
   }

}
