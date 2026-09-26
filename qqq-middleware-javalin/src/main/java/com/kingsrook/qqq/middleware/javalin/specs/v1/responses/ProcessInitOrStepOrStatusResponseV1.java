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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessInitOrStepOrStatusOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.SchemaBuilder;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIIncludeProperties;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIOneOf;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.FieldMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.FrontendStep;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.ProcessMetaDataAdjustment;
import com.kingsrook.qqq.openapi.model.Schema;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessInitOrStepOrStatusResponseV1 implements ProcessInitOrStepOrStatusOutputInterface, ToSchema
{
   private TypedResponse typedResponse;



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIOneOf()
   public static sealed class TypedResponse implements ToSchema permits ProcessStepComplete, ProcessStepJobStarted, ProcessStepRunning, ProcessStepError
   {
      @OpenAPIDescription("What kind of response has been received.  Determines what additional fields will be set.")
      private String type;

      @OpenAPIDescription("Unique identifier for a running instance the process.")
      private String processUUID;



      /*******************************************************************************
       ** Getter for type
       **
       *******************************************************************************/
      public String getType()
      {
         return type;
      }



      /*******************************************************************************
       ** Getter for processUUID
       **
       *******************************************************************************/
      public String getProcessUUID()
      {
         return processUUID;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIIncludeProperties(ancestorClasses = { TypedResponse.class })
   @OpenAPIDescription("Data returned after the job is complete (whether it was synchronous, or asynchronous)")
   public static final class ProcessStepComplete extends TypedResponse
   {
      @OpenAPIDescription("Name of the next process step that needs to run (a frontend step).  If there are no more steps in the process, this field will not be included.  ")
      private String nextStep;

      @OpenAPIDescription("Name of the step the process can go back to (restart at, by stepping with isStepBack=true), when the process declares one.  Omitted otherwise.")
      private String backStep;

      @OpenAPIDescription("Current values for fields used by the process.Keys are Strings, values can be any type, as determined by the application & process.")
      private Map<String, Serializable> values;

      @OpenAPIDescription("Changes to be made to the process's metaData.")
      private ProcessMetaDataAdjustment processMetaDataAdjustment;



      /*******************************************************************************
       ** Getter for nextStep
       **
       *******************************************************************************/
      public String getNextStep()
      {
         return nextStep;
      }



      /*******************************************************************************
       ** Getter for backStep
       **
       *******************************************************************************/
      public String getBackStep()
      {
         return backStep;
      }



      /*******************************************************************************
       ** Getter for values
       **
       *******************************************************************************/
      @JsonIgnore // we are doing custom serialization of the values map, so mark as ignore.
      public Map<String, Serializable> getValues()
      {
         return values;
      }



      /*******************************************************************************
       ** Getter for processMetaDataAdjustment
       **
       *******************************************************************************/
      public ProcessMetaDataAdjustment getProcessMetaDataAdjustment()
      {
         return processMetaDataAdjustment;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIIncludeProperties(ancestorClasses = { TypedResponse.class })
   @OpenAPIDescription("In case the backend needs more time, this is a UUID of the background job that has been started.")
   public static final class ProcessStepJobStarted extends TypedResponse
   {
      @OpenAPIDescription("Unique identifier for a running step of the process.  Must be passed into `status` check calls.")
      private String jobUUID;



      /*******************************************************************************
       ** Getter for jobUUID
       **
       *******************************************************************************/
      public String getJobUUID()
      {
         return jobUUID;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIIncludeProperties(ancestorClasses = { TypedResponse.class })
   @OpenAPIDescription("Response to a status check for a backgrounded job.")
   public static final class ProcessStepRunning extends TypedResponse
   {
      @OpenAPIDescription("Status message regarding the running process step.")
      private String message;

      @OpenAPIDescription("Optional indicator of progress (e.g., `current` of `total`, as in (`1 of 10`).")
      private Integer current;

      @OpenAPIDescription("Optional indicator of progress (e.g., `current` of `total`, as in (`1 of 10`).")
      private Integer total;



      /*******************************************************************************
       ** Getter for message
       **
       *******************************************************************************/
      public String getMessage()
      {
         return message;
      }



      /*******************************************************************************
       ** Getter for current
       **
       *******************************************************************************/
      public Integer getCurrent()
      {
         return current;
      }



      /*******************************************************************************
       ** Getter for total
       **
       *******************************************************************************/
      public Integer getTotal()
      {
         return total;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIIncludeProperties(ancestorClasses = { TypedResponse.class })
   @OpenAPIDescription("In case an error is thrown in the backend job.")
   public static final class ProcessStepError extends TypedResponse
   {
      @OpenAPIDescription("Exception message, in case the process step threw an error.")
      private String error;

      @OpenAPIDescription("Optional user-facing exception message, in case the process step threw a user-facing error.")
      private String userFacingError;



      /*******************************************************************************
       ** Getter for error
       **
       *******************************************************************************/
      public String getError()
      {
         return error;
      }



      /*******************************************************************************
       ** Getter for userFacingError
       **
       *******************************************************************************/
      public String getUserFacingError()
      {
         return userFacingError;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setType(Type type)
   {
      this.typedResponse = switch(type)
      {
         case COMPLETE -> new ProcessStepComplete();
         case JOB_STARTED -> new ProcessStepJobStarted();
         case RUNNING -> new ProcessStepRunning();
         case ERROR -> new ProcessStepError();
      };

      this.typedResponse.type = type.toString();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setProcessUUID(String processUUID)
   {
      this.typedResponse.processUUID = processUUID;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setNextStep(String nextStep)
   {
      if(this.typedResponse instanceof ProcessStepComplete complete)
      {
         complete.nextStep = nextStep;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setBackStep(String backStep)
   {
      if(this.typedResponse instanceof ProcessStepComplete complete)
      {
         complete.backStep = backStep;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setValues(Map<String, Serializable> values)
   {
      if(this.typedResponse instanceof ProcessStepComplete complete)
      {
         complete.values = values;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setProcessMetaDataAdjustment(com.kingsrook.qqq.backend.core.model.actions.processes.ProcessMetaDataAdjustment processMetaDataAdjustment)
   {
      if(this.typedResponse instanceof ProcessStepComplete complete)
      {
         if(processMetaDataAdjustment == null)
         {
            complete.processMetaDataAdjustment = null;
         }
         else
         {
            complete.processMetaDataAdjustment = new ProcessMetaDataAdjustment();

            ////////////////////////////////////////////////////////////////////////////
            // an adjustment may carry only fields or only steps (a route change sets //
            // just the step list) - the other part is absent, not an error            //
            ////////////////////////////////////////////////////////////////////////////
            if(processMetaDataAdjustment.getUpdatedFields() != null)
            {
               Map<String, FieldMetaData> updatedFields = processMetaDataAdjustment.getUpdatedFields().entrySet()
                  .stream().collect(Collectors.toMap(e -> e.getKey(), f -> new FieldMetaData(f.getValue())));
               complete.processMetaDataAdjustment.setUpdatedFields(updatedFields);
            }

            if(processMetaDataAdjustment.getUpdatedFrontendStepList() != null)
            {
               List<FrontendStep> updatedFrontendSteps = processMetaDataAdjustment.getUpdatedFrontendStepList()
                  .stream().map(f -> new FrontendStep(f)).toList();
               complete.processMetaDataAdjustment.setUpdatedFrontendStepList(updatedFrontendSteps);
            }
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setJobUUID(String jobUUID)
   {
      if(this.typedResponse instanceof ProcessStepJobStarted jobStarted)
      {
         jobStarted.jobUUID = jobUUID;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setMessage(String message)
   {
      if(this.typedResponse instanceof ProcessStepRunning running)
      {
         running.message = message;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setCurrent(Integer current)
   {
      if(this.typedResponse instanceof ProcessStepRunning running)
      {
         running.current = current;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setTotal(Integer total)
   {
      if(this.typedResponse instanceof ProcessStepRunning running)
      {
         running.total = total;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setError(String errorString)
   {
      if(this.typedResponse instanceof ProcessStepError error)
      {
         error.error = errorString;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setUserFacingError(String userFacingError)
   {
      if(this.typedResponse instanceof ProcessStepError error)
      {
         error.userFacingError = userFacingError;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Schema toSchema()
   {
      return new SchemaBuilder().classToSchema(TypedResponse.class);
   }



   /*******************************************************************************
    ** Getter for typedResponse
    **
    *******************************************************************************/
   public TypedResponse getTypedResponse()
   {
      return typedResponse;
   }

}
