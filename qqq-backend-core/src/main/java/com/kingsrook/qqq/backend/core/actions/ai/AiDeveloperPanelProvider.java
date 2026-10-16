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


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanel;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelBlock;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelContext;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelLink;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelProviderInterface;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelTone;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Record developer panel for the QQQ Assistant (spec QRun-IO/qqq#1054 §5.7):
 ** what the assistant may read and propose for this table and record under the
 ** current user's permissions - the table's exposure, which generic tools apply
 ** and why, each field's exposure - from
 ** {@link AiToolKernel#describeAccess}, plus an "Ask the assistant about this
 ** record" deep link.
 **
 ** Not registered by core: the assistant qbit registers it when it is
 ** installed, with
 ** {@code DeveloperPanelsMetaData.ofOrWithNew(qInstance).withProvider(new QCodeReference(AiDeveloperPanelProvider.class))},
 ** or registers a subclass that overrides {@link #getSettings()} to describe
 ** access with its own kernel settings.
 *******************************************************************************/
public class AiDeveloperPanelProvider implements DeveloperPanelProviderInterface
{
   public static final String ID = "assistant";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getId()
   {
      return (ID);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getTitle()
   {
      return ("Assistant");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer getOrder()
   {
      return (600);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getIcon()
   {
      return ("smart_toy");
   }



   /*******************************************************************************
    ** The kernel settings to describe access with (the assistant's own, when a
    ** subclass provides them; the defaults otherwise).
    *******************************************************************************/
   protected AiToolKernelSettings getSettings()
   {
      return (new AiToolKernelSettings());
   }



   /*******************************************************************************
    ** Registered only where the assistant is installed, so always available.
    *******************************************************************************/
   @Override
   public boolean isAvailable(DeveloperPanelContext context)
   {
      return (context.getTable() != null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public DeveloperPanel build(DeveloperPanelContext context)
   {
      AiToolKernel   kernel = new AiToolKernel(QContext.getQInstance(), getSettings());
      AiAccessReport report = kernel.describeAccess(QContext.getQSession(), context.getTableName(), context.getPrimaryKey());

      AiExposure exposure = report.getTableExposure();
      boolean    exposed  = exposure != null && exposure != AiExposure.NONE;

      DeveloperPanel panel = new DeveloperPanel()
         .withDescription("What the assistant may read and propose for this record, for you.")
         .withStatusLabel(exposure == null ? "Not available" : exposure.name())
         .withStatusTone(exposed ? DeveloperPanelTone.SUCCESS : DeveloperPanelTone.WARNING);

      DeveloperPanelBlock summary = DeveloperPanelBlock.keyValues("Access")
         .withItem("Table exposure", exposure == null ? "" : exposure.name())
         .withItem("Record readable", report.getRecordReadable() == null ? "" : (report.getRecordReadable() ? "yes" : "no"));
      if(StringUtils.hasContent(report.getReason()))
      {
         summary.withItem("Reason", report.getReason());
      }
      panel.withBlock(summary);

      DeveloperPanelBlock tools = DeveloperPanelBlock.table("Tools", List.of("Tool", "Kind", "Applies", "Why"));
      for(AiToolApplicability tool : CollectionUtils.nonNullList(report.getTools()))
      {
         tools.withRow(List.of(text(tool.getToolName()), text(tool.getKind()), Boolean.TRUE.equals(tool.getApplies()) ? "yes" : "no", text(tool.getReason())));
      }
      panel.withBlock(tools);

      DeveloperPanelBlock fields = DeveloperPanelBlock.table("Fields", List.of("Field", "Exposure", "Can propose", "Why"));
      for(AiFieldAccess field : CollectionUtils.nonNullList(report.getFields()))
      {
         fields.withRow(List.of(text(Objects.requireNonNullElse(field.getLabel(), field.getFieldName())), text(field.getExposure()), Boolean.TRUE.equals(field.getCanPropose()) ? "yes" : "no", text(field.getReason())));
      }
      panel.withBlock(fields);

      if(exposed && Boolean.TRUE.equals(report.getRecordReadable()) && context.getPrimaryKey() != null)
      {
         String subject = StringUtils.hasContent(report.getRecordLabel()) ? report.getRecordLabel() : (report.getTableLabel() + " " + context.getPrimaryKey());
         String prompt  = URLEncoder.encode("Tell me about " + subject, StandardCharsets.UTF_8).replace("+", "%20");
         String href    = "/app/" + context.getTableName() + "/" + URLEncoder.encode(String.valueOf(context.getPrimaryKey()), StandardCharsets.UTF_8) + "?assistant=open&assistantPrompt=" + prompt;
         panel.withBlock(DeveloperPanelBlock.links(null)
            .withLink(new DeveloperPanelLink().withLabel("Ask the assistant about this record").withHref(href)
               .withDescription("Opens the record with the assistant and a prompt to review before sending.")));
      }

      return (panel);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String text(Object value)
   {
      return (value == null ? "" : String.valueOf(value));
   }

}
