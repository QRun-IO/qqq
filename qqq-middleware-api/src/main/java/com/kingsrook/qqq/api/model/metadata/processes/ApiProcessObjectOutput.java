/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.model.metadata.processes;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.api.actions.GenerateOpenApiSpecAction;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.ExampleWithListValue;
import com.kingsrook.qqq.openapi.model.ExampleWithSingleValue;
import com.kingsrook.qqq.openapi.model.Response;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import org.eclipse.jetty.http.HttpStatus;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiProcessObjectOutput implements ApiProcessOutputInterface
{
   private List<QFieldMetaData> outputFields;

   private String          responseDescription;
   private HttpStatus.Code successResponseCode;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public HttpStatus.Code getSuccessStatusCode(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput)
   {
      return (HttpStatus.Code.OK);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<Integer, Response> getSpecResponses(String apiName)
   {
      Map<String, Schema> properties = new LinkedHashMap<>();
      for(QFieldMetaData outputField : CollectionUtils.nonNullList(outputFields))
      {
         ApiFieldMetaDataContainer apiFieldMetaDataContainer = ApiFieldMetaDataContainer.ofOrNew(outputField);
         ApiFieldMetaData          apiFieldMetaData          = apiFieldMetaDataContainer.getApiFieldMetaData(apiName);

         Object example = null;
         if(apiFieldMetaData != null)
         {
            if(apiFieldMetaData.getExample() instanceof ExampleWithSingleValue exampleWithSingleValue)
            {
               example = exampleWithSingleValue.getValue();
            }
            else if(apiFieldMetaData.getExample() instanceof ExampleWithListValue exampleWithListValue)
            {
               example = exampleWithListValue.getValue();
            }
         }

         properties.put(outputField.getName(), new Schema()
            .withDescription(apiFieldMetaData == null ? null : apiFieldMetaData.getDescription())
            .withExample(example)
            .withNullable(!outputField.getIsRequired())
            .withType(GenerateOpenApiSpecAction.getFieldType(outputField))
         );
      }

      return (MapBuilder.of(
         Objects.requireNonNullElse(successResponseCode, HttpStatus.Code.OK).getCode(),
         new Response()
            .withDescription(ObjectUtils.requireConditionElse(responseDescription, StringUtils::hasContent, "Process has been successfully executed."))
            .withContent(MapBuilder.of(ContentType.JSON, new Content()
               .withSchema(new Schema()
                  .withType(Type.OBJECT)
                  .withProperties(properties))))
      ));
   }



   /*******************************************************************************
    **
    ******************************************************************************/
   @Override
   public Serializable getOutputForProcess(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput)
   {
      LinkedHashMap<String, Serializable> outputMap = new LinkedHashMap<>();

      for(QFieldMetaData outputField : CollectionUtils.nonNullList(getOutputFields()))
      {
         outputMap.put(outputField.getName(), runProcessOutput.getValues().get(outputField.getName()));
      }

      return (outputMap);
   }



   /*******************************************************************************
    ** Getter for outputFields
    *******************************************************************************/
   public List<QFieldMetaData> getOutputFields()
   {
      return (this.outputFields);
   }



   /*******************************************************************************
    ** Setter for outputFields
    *******************************************************************************/
   public void setOutputFields(List<QFieldMetaData> outputFields)
   {
      this.outputFields = outputFields;
   }



   /*******************************************************************************
    ** Fluent setter for outputFields
    *******************************************************************************/
   public ApiProcessObjectOutput withOutputFields(List<QFieldMetaData> outputFields)
   {
      this.outputFields = outputFields;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for a single outputField
    *******************************************************************************/
   public ApiProcessObjectOutput withOutputField(QFieldMetaData outputField)
   {
      if(this.outputFields == null)
      {
         this.outputFields = new ArrayList<>();
      }
      this.outputFields.add(outputField);
      return (this);
   }



   /*******************************************************************************
    ** Getter for responseDescription
    *******************************************************************************/
   public String getResponseDescription()
   {
      return (this.responseDescription);
   }



   /*******************************************************************************
    ** Setter for responseDescription
    *******************************************************************************/
   public void setResponseDescription(String responseDescription)
   {
      this.responseDescription = responseDescription;
   }



   /*******************************************************************************
    ** Fluent setter for responseDescription
    *******************************************************************************/
   public ApiProcessObjectOutput withResponseDescription(String responseDescription)
   {
      this.responseDescription = responseDescription;
      return (this);
   }



   /*******************************************************************************
    ** Getter for successResponseCode
    *******************************************************************************/
   public HttpStatus.Code getSuccessResponseCode()
   {
      return (this.successResponseCode);
   }



   /*******************************************************************************
    ** Setter for successResponseCode
    *******************************************************************************/
   public void setSuccessResponseCode(HttpStatus.Code successResponseCode)
   {
      this.successResponseCode = successResponseCode;
   }



   /*******************************************************************************
    ** Fluent setter for successResponseCode
    *******************************************************************************/
   public ApiProcessObjectOutput withSuccessResponseCode(HttpStatus.Code successResponseCode)
   {
      this.successResponseCode = successResponseCode;
      return (this);
   }

}
