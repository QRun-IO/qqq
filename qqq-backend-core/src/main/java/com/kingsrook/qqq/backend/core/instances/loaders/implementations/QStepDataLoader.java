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

package com.kingsrook.qqq.backend.core.instances.loaders.implementations;


import java.util.Map;
import com.kingsrook.qqq.backend.core.instances.loaders.AbstractMetaDataLoader;
import com.kingsrook.qqq.backend.core.instances.loaders.LoadingContext;
import com.kingsrook.qqq.backend.core.instances.loaders.QMetaDataLoaderException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class QStepDataLoader extends AbstractMetaDataLoader<QStepMetaData>
{
   private static final QLogger LOG = QLogger.getLogger(QStepDataLoader.class);



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QStepMetaData mapToMetaDataObject(QInstance qInstance, Map<String, Object> map, LoadingContext context) throws QMetaDataLoaderException
   {
      String stepType = ValueUtils.getValueAsString(map.get("stepType"));

      if(!StringUtils.hasContent(stepType))
      {
         throw (new QMetaDataLoaderException("stepType was not specified for process step"));
      }

      QStepMetaData step;
      if("backend".equalsIgnoreCase(stepType))
      {
         step = new QBackendStepMetaData();
         reflectivelyMap(qInstance, step, map, context);
      }
      else if("frontend".equalsIgnoreCase(stepType))
      {
         step = new QFrontendStepMetaData();
         reflectivelyMap(qInstance, step, map, context);
      }
      // todo - we have custom factory methods for this, so, maybe needs all custom loader?
      // else if("stateMachine".equalsIgnoreCase(stepType))
      // {
      //    step = new QStateMachineStep();
      //    reflectivelyMap(qInstance, step, map, context);
      // }
      else
      {
         throw (new QMetaDataLoaderException("Unsupported step stepType: " + stepType));
      }

      return (step);
   }

}
