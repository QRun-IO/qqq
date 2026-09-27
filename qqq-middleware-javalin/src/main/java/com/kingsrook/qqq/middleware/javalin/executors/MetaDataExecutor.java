/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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


import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataOutput;
import com.kingsrook.qqq.middleware.javalin.executors.io.MetaDataInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.MetaDataOutputInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class MetaDataExecutor extends AbstractMiddlewareExecutor<MetaDataInput, MetaDataOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(MetaDataInput input, MetaDataOutputInterface output) throws QException
   {
      com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput actionInput = new com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput();

      actionInput.setMiddlewareName(input.getMiddlewareName());
      actionInput.setMiddlewareVersion(input.getMiddlewareVersion());
      actionInput.setFrontendName(input.getFrontendName());
      actionInput.setFrontendVersion(input.getFrontendVersion());
      actionInput.setApplicationName(input.getApplicationName());
      actionInput.setApplicationVersion(input.getApplicationVersion());

      MetaDataAction metaDataAction = new MetaDataAction();
      MetaDataOutput metaDataOutput = metaDataAction.execute(actionInput);
      output.setMetaDataOutput(metaDataOutput);
   }

}
