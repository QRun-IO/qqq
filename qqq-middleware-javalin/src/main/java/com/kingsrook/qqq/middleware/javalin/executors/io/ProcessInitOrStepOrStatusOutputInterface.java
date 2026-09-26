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

package com.kingsrook.qqq.middleware.javalin.executors.io;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessMetaDataAdjustment;


/*******************************************************************************
 **
 *******************************************************************************/
public interface ProcessInitOrStepOrStatusOutputInterface extends AbstractMiddlewareOutputInterface
{


   /***************************************************************************
    **
    ***************************************************************************/
   enum Type
   {
      COMPLETE, JOB_STARTED, RUNNING, ERROR;
   }


   /*******************************************************************************
    ** Setter for type
    *******************************************************************************/
   void setType(Type type);

   /*******************************************************************************
    ** Setter for processUUID
    *******************************************************************************/
   void setProcessUUID(String processUUID);

   /*******************************************************************************
    ** Setter for nextStep
    *******************************************************************************/
   void setNextStep(String nextStep);

   /*******************************************************************************
    ** Setter for backStep - the step a frontend may go back to (restart at).
    *******************************************************************************/
   void setBackStep(String backStep);

   /*******************************************************************************
    ** Setter for values
    *******************************************************************************/
   void setValues(Map<String, Serializable> values);

   /*******************************************************************************
    ** Setter for jobUUID
    *******************************************************************************/
   void setJobUUID(String jobUUID);

   /*******************************************************************************
    ** Setter for message
    *******************************************************************************/
   void setMessage(String message);

   /*******************************************************************************
    ** Setter for current
    *******************************************************************************/
   void setCurrent(Integer current);

   /*******************************************************************************
    ** Setter for total
    *******************************************************************************/
   void setTotal(Integer total);

   /*******************************************************************************
    ** Setter for error
    *******************************************************************************/
   void setError(String error);

   /*******************************************************************************
    ** Setter for userFacingError
    *******************************************************************************/
   void setUserFacingError(String userFacingError);

   /*******************************************************************************
    ** Setter for processMetaDataAdjustment
    *******************************************************************************/
   void setProcessMetaDataAdjustment(ProcessMetaDataAdjustment processMetaDataAdjustment);
}
