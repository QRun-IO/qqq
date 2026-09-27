/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.api.actions;


import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.api.model.metadata.APIBackendMetaData;


/*******************************************************************************
 ** Base class for all Backend-module-API Actions
 *******************************************************************************/
public abstract class AbstractAPIAction
{
   protected APIBackendMetaData backendMetaData;
   protected BaseAPIActionUtil  apiActionUtil;
   protected QSession           session;



   /*******************************************************************************
    ** Setup the s3 utils object to be used for this action.
    *******************************************************************************/
   public void preAction(AbstractTableActionInput actionInput)
   {
      QBackendMetaData baseBackendMetaData = QContext.getQInstance().getBackendForTable(actionInput.getTableName());
      this.backendMetaData = (APIBackendMetaData) baseBackendMetaData;
      this.session = QContext.getQSession();

      if(backendMetaData.getActionUtil() != null)
      {
         apiActionUtil = QCodeLoader.getAdHoc(BaseAPIActionUtil.class, backendMetaData.getActionUtil());
      }
      else
      {
         apiActionUtil = new BaseAPIActionUtil();
      }

      apiActionUtil.setBackendMetaData(this.backendMetaData);
      apiActionUtil.setActionInput(actionInput);
   }

}

