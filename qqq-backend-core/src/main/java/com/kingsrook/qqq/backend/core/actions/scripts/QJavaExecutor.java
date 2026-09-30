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

package com.kingsrook.qqq.backend.core.actions.scripts;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.QCodeExecutionLoggerInterface;
import com.kingsrook.qqq.backend.core.exceptions.QCodeException;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** Java
 *******************************************************************************/
public class QJavaExecutor implements QCodeExecutor
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Serializable execute(QCodeReference codeReference, Map<String, Serializable> inputContext, QCodeExecutionLoggerInterface executionLogger) throws QCodeException
   {
      Map<String, Object> context = new HashMap<>(inputContext);
      if(!context.containsKey("logger"))
      {
         context.put("logger", executionLogger);
      }

      Serializable output;
      try
      {
         @SuppressWarnings("unchecked")
         Function<Map<String, Object>, Serializable> function = QCodeLoader.getAdHoc(Function.class, codeReference);
         output = function.apply(context);
      }
      catch(Exception e)
      {
         QCodeException qCodeException = new QCodeException("Error executing script", e);
         throw (qCodeException);
      }

      return (output);
   }

}
