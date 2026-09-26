/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.processes;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionOutputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.esb.metadata.EsbAppMetaDataProducer;


/*******************************************************************************
 * Shared metadata shape for the nine ESB management processes.
 ******************************************************************************/
public abstract class EsbOperateProcessMetaDataProducer extends MetaDataProducer<QProcessMetaData>
{
   private final String  name;
   private final String  label;
   private final Boolean delete;
   private final List<String> inputNames;
   private final Class<? extends BackendStep> stepClass;



   /*******************************************************************************
    **
    *******************************************************************************/
   protected EsbOperateProcessMetaDataProducer(String name, String label, Boolean delete, Class<? extends BackendStep> stepClass, String... inputNames)
   {
      this.name = name;
      this.label = label;
      this.delete = delete;
      this.stepClass = stepClass;
      this.inputNames = List.of(inputNames);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      List<QFieldMetaData> inputs = new ArrayList<>();
      for(String inputName : inputNames)
      {
         QFieldType type = switch(inputName)
         {
            case "all" -> QFieldType.BOOLEAN;
            case "olderThan" -> QFieldType.DATE_TIME;
            default -> QFieldType.STRING;
         };
         inputs.add(new QFieldMetaData(inputName, type));
      }

      String permissionBaseName = delete ? EsbAppMetaDataProducer.DELETE_PERMISSION_BASE_NAME : EsbAppMetaDataProducer.OPERATE_PERMISSION_BASE_NAME;
      return (new QProcessMetaData()
         .withName(name)
         .withLabel(label)
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION).withPermissionBaseName(permissionBaseName))
         .withStep(new QBackendStepMetaData()
            .withName("operate")
            .withCode(new QCodeReference(stepClass))
            .withInputData(new QFunctionInputMetaData().withFieldList(inputs))
            .withOutputMetaData(new QFunctionOutputMetaData().withFieldList(List.of(
               new QFieldMetaData("count", QFieldType.INTEGER),
               new QFieldMetaData("message", QFieldType.STRING))))));
   }
}
