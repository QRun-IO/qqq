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
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.QCodeExecutionLoggerInterface;
import com.kingsrook.qqq.backend.core.exceptions.QCodeException;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** Interface to be implemented by language-specific code executors, e.g., in
 ** qqq-language-support-${languageName} maven modules.
 *******************************************************************************/
public interface QCodeExecutor
{

   /*******************************************************************************
    **
    *******************************************************************************/
   Serializable execute(QCodeReference codeReference, Map<String, Serializable> inputContext, QCodeExecutionLoggerInterface executionLogger) throws QCodeException;

   /*******************************************************************************
    ** Process an object from the script's language/runtime into a (more) native java object.
    ** e.g., a Nashorn ScriptObjectMirror will end up as a "primitive", or a List or Map of such
    **
    *******************************************************************************/
   default Object convertObjectToJava(Object object) throws QCodeException
   {
      return (object);
   }

   /*******************************************************************************
    ** Convert a native java object into one for the script's language/runtime.
    ** e.g., a java Instant to a Nashorn Date
    **
    *******************************************************************************/
   default Object convertJavaObject(Object object, Object requestedTypeHint) throws QCodeException
   {
      return (object);
   }

}
