/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
