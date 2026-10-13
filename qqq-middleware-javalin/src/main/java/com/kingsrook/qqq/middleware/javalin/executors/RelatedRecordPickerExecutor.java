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

package com.kingsrook.qqq.middleware.javalin.executors;


import com.kingsrook.qqq.backend.core.actions.tables.picker.RelatedRecordPickerAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.picker.RelatedRecordPickerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.picker.RelatedRecordPickerOutput;
import com.kingsrook.qqq.middleware.javalin.executors.io.RelatedRecordPickerMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.RelatedRecordPickerOutputInterface;


/*******************************************************************************
 ** Runs a relatedRecordPicker route's operation through
 ** RelatedRecordPickerAction (which does every permission and security check).
 *******************************************************************************/
public class RelatedRecordPickerExecutor extends AbstractMiddlewareExecutor<RelatedRecordPickerMiddlewareInput, RelatedRecordPickerOutputInterface>
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void execute(RelatedRecordPickerMiddlewareInput input, RelatedRecordPickerOutputInterface output) throws QException
   {
      RelatedRecordPickerAction action      = new RelatedRecordPickerAction();
      RelatedRecordPickerInput  pickerInput = input.getPickerInput();
      RelatedRecordPickerOutput pickerOutput = switch(input.getOperation())
      {
         case QUERY -> action.query(pickerInput);
         case PREPARE -> action.prepare(pickerInput);
         case ADD -> action.add(pickerInput);
         case REMOVE -> action.remove(pickerInput);
      };
      output.setPickerOutput(pickerOutput);
   }
}
