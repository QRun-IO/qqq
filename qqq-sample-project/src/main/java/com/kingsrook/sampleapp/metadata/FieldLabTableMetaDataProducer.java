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

package com.kingsrook.sampleapp.metadata;


import java.math.BigDecimal;
import java.util.List;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.CaseChangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueRangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.WhiteSpaceBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;


/*******************************************************************************
 ** Editable examples of field types and normalization/validation policies.
 *******************************************************************************/
public class FieldLabTableMetaDataProducer extends MetaDataProducer<QTableMetaData>
{
   public static final String NAME = "fieldLab";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QTableMetaData produce(QInstance instance)
   {
      QTableMetaData table = new QTableMetaData()
         .withName(NAME).withLabel("Field Lab")
         .withBackendName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("field_lab"))
         .withPrimaryKeyField("id").withRecordLabelFields("name")
         .withUniqueKey(new UniqueKey("name"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withIsRequired(true).withMaxLength(80))
         .withField(new QFieldMetaData("longValue", QFieldType.LONG))
         .withField(new QFieldMetaData("decimalValue", QFieldType.DECIMAL))
         .withField(new QFieldMetaData("booleanValue", QFieldType.BOOLEAN).withDefaultValue(true))
         .withField(new QFieldMetaData("dateValue", QFieldType.DATE))
         .withField(new QFieldMetaData("timeValue", QFieldType.TIME))
         .withField(new QFieldMetaData("dateTimeValue", QFieldType.DATE_TIME))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("textValue", QFieldType.TEXT))
         .withField(new QFieldMetaData("htmlValue", QFieldType.HTML))
         .withField(new QFieldMetaData("passwordValue", QFieldType.PASSWORD))
         .withField(new QFieldMetaData("blobValue", QFieldType.BLOB))
         .withField(new QFieldMetaData("truncateValue", QFieldType.STRING).withMaxLength(8).withBehavior(ValueTooLongBehavior.TRUNCATE))
         .withField(new QFieldMetaData("ellipsisValue", QFieldType.STRING).withMaxLength(8).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS))
         .withField(new QFieldMetaData("rejectLongValue", QFieldType.STRING).withMaxLength(8).withBehavior(ValueTooLongBehavior.ERROR))
         .withField(new QFieldMetaData("passThroughValue", QFieldType.STRING).withMaxLength(8).withBehavior(ValueTooLongBehavior.PASS_THROUGH))
         .withField(new QFieldMetaData("upperValue", QFieldType.STRING).withBehavior(CaseChangeBehavior.TO_UPPER_CASE))
         .withField(new QFieldMetaData("lowerValue", QFieldType.STRING).withBehavior(CaseChangeBehavior.TO_LOWER_CASE))
         .withField(new QFieldMetaData("unchangedValue", QFieldType.STRING).withBehavior(CaseChangeBehavior.NONE).withBehavior(WhiteSpaceBehavior.NONE))
         .withField(new QFieldMetaData("trimValue", QFieldType.STRING).withBehavior(WhiteSpaceBehavior.TRIM))
         .withField(new QFieldMetaData("trimLeftValue", QFieldType.STRING).withBehavior(WhiteSpaceBehavior.TRIM_LEFT))
         .withField(new QFieldMetaData("trimRightValue", QFieldType.STRING).withBehavior(WhiteSpaceBehavior.TRIM_RIGHT))
         .withField(new QFieldMetaData("removeSpaceValue", QFieldType.STRING).withBehavior(WhiteSpaceBehavior.REMOVE_ALL_WHITESPACE))
         .withField(new QFieldMetaData("boundedValue", QFieldType.DECIMAL).withBehavior(new ValueRangeBehavior().withMinValue(0).withMaxValue(100)))
         .withField(new QFieldMetaData("clippedValue", QFieldType.DECIMAL).withBehavior(new ValueRangeBehavior()
            .withMin(0, false, ValueRangeBehavior.Behavior.CLIP, new BigDecimal("0.01"))
            .withMax(100, false, ValueRangeBehavior.Behavior.CLIP, new BigDecimal("0.01"))));

      QInstanceEnricher.setInferredFieldBackendNames(table);
      table.addSection(new QFieldSection("identity", "Identity", new QIcon("badge"), Tier.T1, List.of("id", "name", "createDate", "modifyDate")));
      table.addSection(new QFieldSection("types", "Field Types", new QIcon("data_object"), Tier.T2,
         List.of("longValue", "decimalValue", "booleanValue", "dateValue", "timeValue", "dateTimeValue", "textValue", "htmlValue", "passwordValue", "blobValue")));
      table.addSection(new QFieldSection("length", "Length Policies", new QIcon("text_fields"), Tier.T2,
         List.of("truncateValue", "ellipsisValue", "rejectLongValue", "passThroughValue")));
      table.addSection(new QFieldSection("normalization", "Case and Whitespace", new QIcon("abc"), Tier.T2,
         List.of("upperValue", "lowerValue", "unchangedValue", "trimValue", "trimLeftValue", "trimRightValue", "removeSpaceValue")));
      table.addSection(new QFieldSection("range", "Numeric Bounds", new QIcon("numbers"), Tier.T2, List.of("boundedValue", "clippedValue")));
      return table;
   }
}
