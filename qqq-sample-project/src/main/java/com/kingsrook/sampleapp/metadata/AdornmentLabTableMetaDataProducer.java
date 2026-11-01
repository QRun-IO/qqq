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

package com.kingsrook.sampleapp.metadata;


import java.util.List;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.CaseChangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpRole;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.sampleapp.metadata.widgetsdashboard.SampleHTMLWidgetMetaDataProducer;


/*******************************************************************************
 ** A seeded fixture for client authors (QRun-IO/qqq#1350), beside the Field
 ** Lab (whose empty table the sample's contract tests own): one field per field
 ** adornment, a hidden field and a hidden section, help content on the table,
 ** a section and a field, an exposed join (carrier), and a composite-key
 ** association (tags, joined on both id and labKey).
 ** AdornmentLabRelationsMetaDataProducer defines the child tables and joins,
 ** and the locker whose secrets the mock user may not read.
 *******************************************************************************/
public class AdornmentLabTableMetaDataProducer extends MetaDataProducer<QTableMetaData>
{
   public static final String NAME = "adornmentLab";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QTableMetaData produce(QInstance instance)
   {
      QTableMetaData table = new QTableMetaData()
         .withName(NAME).withLabel("Adornment Lab")
         .withBackendName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("adornment_lab"))
         .withPrimaryKeyField("id").withRecordLabelFields("name")
         .withUniqueKey(new UniqueKey("labKey"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withIsRequired(true).withMaxLength(80))
         .withField(new QFieldMetaData("labKey", QFieldType.STRING).withIsRequired(true).withMaxLength(40).withBehavior(CaseChangeBehavior.TO_UPPER_CASE))

         ////////////////////////////////////
         // one field per adornment type   //
         ////////////////////////////////////
         .withField(new QFieldMetaData("statusChip", QFieldType.STRING).withLabel("Status (CHIP)")
            .withHelpContent(new QHelpContent("The record's status, shown as a coloured chip.").withRole(QHelpRole.ALL_SCREENS))
            .withFieldAdornment(new FieldAdornment(AdornmentType.CHIP)
               .withValue(AdornmentType.ChipValues.colorValue("ACTIVE", AdornmentType.ChipValues.COLOR_SUCCESS))
               .withValue(AdornmentType.ChipValues.colorValue("PENDING", AdornmentType.ChipValues.COLOR_WARNING))
               .withValue(AdornmentType.ChipValues.colorValue("FAILED", AdornmentType.ChipValues.COLOR_ERROR))
               .withValue(AdornmentType.ChipValues.iconValue("ACTIVE", "check_circle"))))
         .withField(new QFieldMetaData("secretToken", QFieldType.STRING).withLabel("Secret Token (REVEAL)")
            .withFieldAdornment(new FieldAdornment(AdornmentType.REVEAL)))
         .withField(new QFieldMetaData("renderedHtml", QFieldType.TEXT).withLabel("Rendered HTML (RENDER_HTML)")
            .withFieldAdornment(new FieldAdornment(AdornmentType.RENDER_HTML)))
         .withField(new QFieldMetaData("scriptCode", QFieldType.TEXT).withLabel("Script Code (CODE_EDITOR)")
            .withFieldAdornment(new FieldAdornment(AdornmentType.CODE_EDITOR).withValue(AdornmentType.CodeEditorValues.languageMode("javascript"))))
         .withField(new QFieldMetaData("errorMessage", QFieldType.STRING).withLabel("Error Message (ERROR)")
            .withFieldAdornment(new FieldAdornment(AdornmentType.ERROR)))
         .withField(new QFieldMetaData("attachment", QFieldType.BLOB).withLabel("Attachment (FILE_DOWNLOAD)")
            .withFieldAdornment(new FieldAdornment(AdornmentType.FILE_DOWNLOAD)
               .withValue(AdornmentType.FileDownloadValues.FILE_NAME_FIELD, "attachmentFileName")
               .withValue(AdornmentType.FileDownloadValues.DEFAULT_MIME_TYPE, "text/plain")))
         .withField(new QFieldMetaData("attachmentFileName", QFieldType.STRING).withLabel("Attachment File Name"))
         .withField(new QFieldMetaData("imageUrl", QFieldType.STRING).withLabel("Image URL (FILE_PREVIEW)")
            .withFieldAdornment(AdornmentType.FilePreviewValues.newFieldAdornment()
               .withValue(AdornmentType.FilePreviewValues.DEFAULT_MIME_TYPE, "image/png")
               .withValue(AdornmentType.FilePreviewValues.THUMBNAIL_MAX_SIZE, 64)))
         .withField(new QFieldMetaData("uploadValue", QFieldType.BLOB).withLabel("Upload (FILE_UPLOAD)")
            .withFieldAdornment(AdornmentType.FileUploadAdornment.newFieldAdornment()
               .withValue(AdornmentType.FileUploadAdornment.formatDragAndDrop())
               .withValue(AdornmentType.FileUploadAdornment.widthFull())))
         .withField(new QFieldMetaData("widgetValue", QFieldType.STRING).withLabel("Widget Value (WIDGET)")
            .withFieldAdornment(new FieldAdornment(AdornmentType.WIDGET).withValue(AdornmentType.WidgetValues.WIDGET_NAME, SampleHTMLWidgetMetaDataProducer.NAME)))
         .withField(new QFieldMetaData("tooltipValue", QFieldType.STRING).withLabel("Tooltip Value (TOOLTIP)")
            .withFieldAdornment(new FieldAdornment(AdornmentType.TOOLTIP).withValue(AdornmentType.TooltipValues.STATIC_TEXT, "A static tooltip from the TOOLTIP adornment.")))
         .withField(new QFieldMetaData("sizeValue", QFieldType.STRING).withLabel("Size Value (SIZE)")
            .withFieldAdornment(AdornmentType.Size.LARGE.toAdornment()))
         .withField(new QFieldMetaData("carrierId", QFieldType.INTEGER).withLabel("Carrier (LINK)")
            .withFieldAdornment(new FieldAdornment(AdornmentType.LINK).withValue(AdornmentType.LinkValues.TO_RECORD_FROM_TABLE, SampleMetaDataProvider.TABLE_NAME_CARRIER)))
         .withField(new QFieldMetaData("hiddenValue", QFieldType.STRING).withLabel("Hidden Value").withIsHidden(true))
         .withField(new QFieldMetaData("internalNote", QFieldType.STRING).withLabel("Internal Note"))

         .withHelpContent(QHelpContent.SLOT_HELP, new QHelpContent("The Adornment Lab shows every field adornment, for trying out clients.").withRole(QHelpRole.ALL_SCREENS))
         .withExposedJoin(new ExposedJoin().withLabel("Carrier").withJoinTable(SampleMetaDataProvider.TABLE_NAME_CARRIER)
            .withJoinPath(List.of(AdornmentLabRelationsMetaDataProducer.JOIN_CARRIER)))
         .withAssociation(new Association().withName("tags").withAssociatedTableName(AdornmentLabRelationsMetaDataProducer.TABLE_TAG)
            .withJoinName(AdornmentLabRelationsMetaDataProducer.JOIN_TAG));

      QInstanceEnricher.setInferredFieldBackendNames(table);
      table.addSection(new QFieldSection("identity", "Identity", new QIcon("badge"), Tier.T1, List.of("id", "name", "labKey")));
      table.addSection(new QFieldSection("adornments", "Adornments", new QIcon("palette"), Tier.T2,
         List.of("statusChip", "secretToken", "renderedHtml", "scriptCode", "errorMessage", "attachment", "attachmentFileName", "imageUrl", "uploadValue",
            "widgetValue", "tooltipValue", "sizeValue", "carrierId"))
         .withHelpContent(new QHelpContent("Each field here carries one adornment, named in its label.").withRole(QHelpRole.ALL_SCREENS)));
      table.addSection(new QFieldSection("internals", "Internals", new QIcon("visibility_off"), Tier.T3, List.of("internalNote")).withIsHidden(true));
      return table;
   }
}
