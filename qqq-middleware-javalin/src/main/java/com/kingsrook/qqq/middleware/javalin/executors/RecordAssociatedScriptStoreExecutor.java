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


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordAssociatedScriptStoreInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordAssociatedScriptStoreOutputInterface;
import com.kingsrook.qqq.middleware.javalin.executors.utils.RecordDeveloperModeUtils;


/*******************************************************************************
 ** Store a new revision of a record's associated script - requiring EDIT
 ** permission on the record's table, as the legacy developer route does.
 *******************************************************************************/
public class RecordAssociatedScriptStoreExecutor extends AbstractMiddlewareExecutor<RecordAssociatedScriptStoreInput, RecordAssociatedScriptStoreOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(RecordAssociatedScriptStoreInput input, RecordAssociatedScriptStoreOutputInterface output) throws QException
   {
      StoreAssociatedScriptInput storeAssociatedScriptInput = new StoreAssociatedScriptInput();
      storeAssociatedScriptInput.setTableName(input.getTableName());
      storeAssociatedScriptInput.setRecordPrimaryKey(input.getPrimaryKey());
      storeAssociatedScriptInput.setFieldName(input.getFieldName());
      storeAssociatedScriptInput.setCode(input.getContents());
      storeAssociatedScriptInput.setCommitMessage(input.getCommitMessage());

      output.setStoreAssociatedScriptOutput(RecordDeveloperModeUtils.storeAssociatedScript(storeAssociatedScriptInput));
   }

}
