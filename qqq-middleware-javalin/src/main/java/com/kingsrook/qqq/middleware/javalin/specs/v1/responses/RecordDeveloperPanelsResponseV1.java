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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanel;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordDeveloperPanelsOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/*******************************************************************************
 ** Response of the record developer panels endpoint.
 *******************************************************************************/
public class RecordDeveloperPanelsResponseV1 implements RecordDeveloperPanelsOutputInterface, ToSchema
{
   @OpenAPIDescription("""
      The panels that the instance's developer panel providers contribute for this record, sorted by order then title.  Each panel has an id, title, order, optional icon, description and status badge (statusLabel, statusTone: INFO, SUCCESS, WARNING or ERROR), and either an error message (when its provider failed) or a list of blocks.  Each block has a type and a label; KEY_VALUES blocks use items (label, value, monospace, href), TEXT blocks use text and tone, CODE blocks use language and code, TABLE blocks use columns and rows (lists of display strings), LINKS blocks use links (label, href, description, external) and ACTIONS blocks use actions (label, processName, tableName, includeRecordId, description).  Hrefs are app-relative (e.g., /app/table/1) or absolute.""")
   @OpenAPIListItems(value = DeveloperPanel.class)
   private List<DeveloperPanel> panels;



   /*******************************************************************************
    ** Setter for panels
    *******************************************************************************/
   @Override
   public void setPanels(List<DeveloperPanel> panels)
   {
      this.panels = panels;
   }



   /*******************************************************************************
    ** Fluent setter for panels
    *******************************************************************************/
   public RecordDeveloperPanelsResponseV1 withPanels(List<DeveloperPanel> panels)
   {
      this.panels = panels;
      return (this);
   }



   /*******************************************************************************
    ** Getter for panels
    *******************************************************************************/
   public List<DeveloperPanel> getPanels()
   {
      return (this.panels);
   }

}
