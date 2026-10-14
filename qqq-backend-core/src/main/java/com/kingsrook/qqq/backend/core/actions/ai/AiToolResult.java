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

package com.kingsrook.qqq.backend.core.actions.ai;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/*******************************************************************************
 ** The result of running one AI tool call through the {@link AiToolKernel}.
 **
 ** modelContent is what goes back to the model (as the tool_result text).  It
 ** is always wrapped in a data envelope, like:
 **    <qqq_data source="query_records" table="blog_post" trust="untrusted">{...}</qqq_data>
 ** with any qqq_data tags inside the data neutralized.
 **
 ** The other members are for the UI and for audit: a short human summary, the
 ** record references and render payload, the redactions applied, and the
 ** permission check that was made.
 *******************************************************************************/
public class AiToolResult
{
   private String              toolCallId;
   private String              toolName;
   private AiToolOutcome       outcome;
   private String              modelContent;
   private String              summary;
   private Integer             rowCount;
   private String              tableName;
   private String              processName;
   private String              widgetName;
   private List<AiRecordRef>   recordRefs  = new ArrayList<>();
   private Map<String, Object> render;
   private String              navigateHref;
   private List<String>        redactions  = new ArrayList<>();
   private String              permissionCheck;
   private Long                durationMs;


   /*******************************************************************************
    ** Getter for toolCallId
    *******************************************************************************/
   public String getToolCallId()
   {
      return (this.toolCallId);
   }



   /*******************************************************************************
    ** Setter for toolCallId
    *******************************************************************************/
   public void setToolCallId(String toolCallId)
   {
      this.toolCallId = toolCallId;
   }



   /*******************************************************************************
    ** Fluent setter for toolCallId
    *******************************************************************************/
   public AiToolResult withToolCallId(String toolCallId)
   {
      this.toolCallId = toolCallId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for toolName
    *******************************************************************************/
   public String getToolName()
   {
      return (this.toolName);
   }



   /*******************************************************************************
    ** Setter for toolName
    *******************************************************************************/
   public void setToolName(String toolName)
   {
      this.toolName = toolName;
   }



   /*******************************************************************************
    ** Fluent setter for toolName
    *******************************************************************************/
   public AiToolResult withToolName(String toolName)
   {
      this.toolName = toolName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for outcome
    *******************************************************************************/
   public AiToolOutcome getOutcome()
   {
      return (this.outcome);
   }



   /*******************************************************************************
    ** Setter for outcome
    *******************************************************************************/
   public void setOutcome(AiToolOutcome outcome)
   {
      this.outcome = outcome;
   }



   /*******************************************************************************
    ** Fluent setter for outcome
    *******************************************************************************/
   public AiToolResult withOutcome(AiToolOutcome outcome)
   {
      this.outcome = outcome;
      return (this);
   }



   /*******************************************************************************
    ** Getter for modelContent
    *******************************************************************************/
   public String getModelContent()
   {
      return (this.modelContent);
   }



   /*******************************************************************************
    ** Setter for modelContent
    *******************************************************************************/
   public void setModelContent(String modelContent)
   {
      this.modelContent = modelContent;
   }



   /*******************************************************************************
    ** Fluent setter for modelContent
    *******************************************************************************/
   public AiToolResult withModelContent(String modelContent)
   {
      this.modelContent = modelContent;
      return (this);
   }



   /*******************************************************************************
    ** Getter for summary
    *******************************************************************************/
   public String getSummary()
   {
      return (this.summary);
   }



   /*******************************************************************************
    ** Setter for summary
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    ** Fluent setter for summary
    *******************************************************************************/
   public AiToolResult withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rowCount
    *******************************************************************************/
   public Integer getRowCount()
   {
      return (this.rowCount);
   }



   /*******************************************************************************
    ** Setter for rowCount
    *******************************************************************************/
   public void setRowCount(Integer rowCount)
   {
      this.rowCount = rowCount;
   }



   /*******************************************************************************
    ** Fluent setter for rowCount
    *******************************************************************************/
   public AiToolResult withRowCount(Integer rowCount)
   {
      this.rowCount = rowCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public AiToolResult withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for processName
    *******************************************************************************/
   public String getProcessName()
   {
      return (this.processName);
   }



   /*******************************************************************************
    ** Setter for processName
    *******************************************************************************/
   public void setProcessName(String processName)
   {
      this.processName = processName;
   }



   /*******************************************************************************
    ** Fluent setter for processName
    *******************************************************************************/
   public AiToolResult withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for widgetName
    *******************************************************************************/
   public String getWidgetName()
   {
      return (this.widgetName);
   }



   /*******************************************************************************
    ** Setter for widgetName
    *******************************************************************************/
   public void setWidgetName(String widgetName)
   {
      this.widgetName = widgetName;
   }



   /*******************************************************************************
    ** Fluent setter for widgetName
    *******************************************************************************/
   public AiToolResult withWidgetName(String widgetName)
   {
      this.widgetName = widgetName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordRefs
    *******************************************************************************/
   public List<AiRecordRef> getRecordRefs()
   {
      return (this.recordRefs);
   }



   /*******************************************************************************
    ** Setter for recordRefs
    *******************************************************************************/
   public void setRecordRefs(List<AiRecordRef> recordRefs)
   {
      this.recordRefs = recordRefs;
   }



   /*******************************************************************************
    ** Fluent setter for recordRefs
    *******************************************************************************/
   public AiToolResult withRecordRefs(List<AiRecordRef> recordRefs)
   {
      this.recordRefs = recordRefs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for render
    *******************************************************************************/
   public Map<String, Object> getRender()
   {
      return (this.render);
   }



   /*******************************************************************************
    ** Setter for render
    *******************************************************************************/
   public void setRender(Map<String, Object> render)
   {
      this.render = render;
   }



   /*******************************************************************************
    ** Fluent setter for render
    *******************************************************************************/
   public AiToolResult withRender(Map<String, Object> render)
   {
      this.render = render;
      return (this);
   }



   /*******************************************************************************
    ** Getter for navigateHref
    *******************************************************************************/
   public String getNavigateHref()
   {
      return (this.navigateHref);
   }



   /*******************************************************************************
    ** Setter for navigateHref
    *******************************************************************************/
   public void setNavigateHref(String navigateHref)
   {
      this.navigateHref = navigateHref;
   }



   /*******************************************************************************
    ** Fluent setter for navigateHref
    *******************************************************************************/
   public AiToolResult withNavigateHref(String navigateHref)
   {
      this.navigateHref = navigateHref;
      return (this);
   }



   /*******************************************************************************
    ** Getter for redactions
    *******************************************************************************/
   public List<String> getRedactions()
   {
      return (this.redactions);
   }



   /*******************************************************************************
    ** Setter for redactions
    *******************************************************************************/
   public void setRedactions(List<String> redactions)
   {
      this.redactions = redactions;
   }



   /*******************************************************************************
    ** Fluent setter for redactions
    *******************************************************************************/
   public AiToolResult withRedactions(List<String> redactions)
   {
      this.redactions = redactions;
      return (this);
   }



   /*******************************************************************************
    ** Getter for permissionCheck
    *******************************************************************************/
   public String getPermissionCheck()
   {
      return (this.permissionCheck);
   }



   /*******************************************************************************
    ** Setter for permissionCheck
    *******************************************************************************/
   public void setPermissionCheck(String permissionCheck)
   {
      this.permissionCheck = permissionCheck;
   }



   /*******************************************************************************
    ** Fluent setter for permissionCheck
    *******************************************************************************/
   public AiToolResult withPermissionCheck(String permissionCheck)
   {
      this.permissionCheck = permissionCheck;
      return (this);
   }



   /*******************************************************************************
    ** Getter for durationMs
    *******************************************************************************/
   public Long getDurationMs()
   {
      return (this.durationMs);
   }



   /*******************************************************************************
    ** Setter for durationMs
    *******************************************************************************/
   public void setDurationMs(Long durationMs)
   {
      this.durationMs = durationMs;
   }



   /*******************************************************************************
    ** Fluent setter for durationMs
    *******************************************************************************/
   public AiToolResult withDurationMs(Long durationMs)
   {
      this.durationMs = durationMs;
      return (this);
   }
}
