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
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist.BulkLoadAssist;
import com.kingsrook.qqq.middleware.javalin.executors.io.BulkLoadValidateInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.BulkLoadValidateOutputInterface;


/*******************************************************************************
 ** Runs BulkLoadAssist.validate: a dry run of a profile through the table's bulk-insert process (nothing is stored).
 *******************************************************************************/
public class BulkLoadValidateExecutor extends AbstractMiddlewareExecutor<BulkLoadValidateInput, BulkLoadValidateOutputInterface>
{

   /***************************************************************************
    ** Permission (the table's bulk-insert process), the upload ref's owner and
    ** the PHI rule are all checked by BulkLoadAssist, as the request's user.
    ***************************************************************************/
   @Override
   public void execute(BulkLoadValidateInput input, BulkLoadValidateOutputInterface output) throws QException
   {
      output.setResult(new BulkLoadAssist().validate(input.getTableName(), input.getUploadRef(), input.getProfile(), BulkLoadAssist.Mode.valueOf(input.getMode() == null ? "SAMPLE" : input.getMode()), input.getRoute()));
   }

}
