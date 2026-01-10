/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.model.metadata.processes;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.api.ApiSupplementType;
import com.kingsrook.qqq.api.model.APIVersionRange;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiProcessMetaData
{
   private String initialVersion;
   private String finalVersion;

   private String  apiProcessName;
   private Boolean isExcluded;
   private Boolean overrideProcessIsHidden;

   private String     path;
   private HttpMethod method;
   private String     summary;
   private String     description;
   private String     tag;

   private AsyncMode asyncMode = AsyncMode.OPTIONAL;

   private ApiProcessInput           input;
   private ApiProcessOutputInterface output;

   private Map<String, QCodeReference> customizers;



   /***************************************************************************
    **
    ***************************************************************************/
   public enum AsyncMode
   {
      NEVER,
      OPTIONAL,
      ALWAYS
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public APIVersionRange getApiVersionRange()
   {
      if(getInitialVersion() == null)
      {
         return APIVersionRange.none();
      }

      return (getFinalVersion() != null
         ? APIVersionRange.betweenAndIncluding(getInitialVersion(), getFinalVersion())
         : APIVersionRange.afterAndIncluding(getInitialVersion()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void enrich(QInstanceEnricher qInstanceEnricher, String apiName, QProcessMetaData process)
   {
      if(!StringUtils.hasContent(getApiProcessName()))
      {
         setApiProcessName(process.getName());
      }

      if(initialVersion != null)
      {
         if(getOutput() instanceof ApiProcessObjectOutput outputObject)
         {
            enrichFieldList(qInstanceEnricher, apiName, outputObject.getOutputFields());
         }

         if(input != null)
         {
            for(ApiProcessInputFieldsContainer fieldsContainer : ListBuilder.of(input.getQueryStringParams(), input.getFormParams(), input.getObjectBodyParams()))
            {
               if(fieldsContainer != null)
               {
                  enrichFieldList(qInstanceEnricher, apiName, fieldsContainer.getFields());
               }
            }
            if(input.getBodyField() != null)
            {
               enrichFieldList(qInstanceEnricher, apiName, List.of(input.getBodyField()));
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enrichFieldList(QInstanceEnricher qInstanceEnricher, String apiName, List<QFieldMetaData> fields)
   {
      for(QFieldMetaData field : CollectionUtils.nonNullList(fields))
      {
         ApiFieldMetaData apiFieldMetaData = ensureFieldHasApiSupplementalMetaData(apiName, field);
         if(apiFieldMetaData.getInitialVersion() == null)
         {
            apiFieldMetaData.setInitialVersion(initialVersion);
         }

         qInstanceEnricher.enrichField(field);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ApiFieldMetaData ensureFieldHasApiSupplementalMetaData(String apiName, QFieldMetaData field)
   {
      if(field.getSupplementalMetaData(ApiSupplementType.NAME) == null)
      {
         field.withSupplementalMetaData(new ApiFieldMetaDataContainer());
      }

      ApiFieldMetaDataContainer apiFieldMetaDataContainer = ApiFieldMetaDataContainer.of(field);
      if(apiFieldMetaDataContainer.getApiFieldMetaData(apiName) == null)
      {
         apiFieldMetaDataContainer.withApiFieldMetaData(apiName, new ApiFieldMetaData());
      }

      return (apiFieldMetaDataContainer.getApiFieldMetaData(apiName));
   }



   /*******************************************************************************
    ** Getter for initialVersion
    *******************************************************************************/
   public String getInitialVersion()
   {
      return (this.initialVersion);
   }



   /*******************************************************************************
    ** Setter for initialVersion
    *******************************************************************************/
   public void setInitialVersion(String initialVersion)
   {
      this.initialVersion = initialVersion;
   }



   /*******************************************************************************
    ** Fluent setter for initialVersion
    *******************************************************************************/
   public ApiProcessMetaData withInitialVersion(String initialVersion)
   {
      this.initialVersion = initialVersion;
      return (this);
   }



   /*******************************************************************************
    ** Getter for finalVersion
    *******************************************************************************/
   public String getFinalVersion()
   {
      return (this.finalVersion);
   }



   /*******************************************************************************
    ** Setter for finalVersion
    *******************************************************************************/
   public void setFinalVersion(String finalVersion)
   {
      this.finalVersion = finalVersion;
   }



   /*******************************************************************************
    ** Fluent setter for finalVersion
    *******************************************************************************/
   public ApiProcessMetaData withFinalVersion(String finalVersion)
   {
      this.finalVersion = finalVersion;
      return (this);
   }



   /*******************************************************************************
    ** Getter for apiProcessName
    *******************************************************************************/
   public String getApiProcessName()
   {
      return (this.apiProcessName);
   }



   /*******************************************************************************
    ** Setter for apiProcessName
    *******************************************************************************/
   public void setApiProcessName(String apiProcessName)
   {
      this.apiProcessName = apiProcessName;
   }



   /*******************************************************************************
    ** Fluent setter for apiProcessName
    *******************************************************************************/
   public ApiProcessMetaData withApiProcessName(String apiProcessName)
   {
      this.apiProcessName = apiProcessName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isExcluded
    *******************************************************************************/
   public Boolean getIsExcluded()
   {
      return (this.isExcluded);
   }



   /*******************************************************************************
    ** Setter for isExcluded
    *******************************************************************************/
   public void setIsExcluded(Boolean isExcluded)
   {
      this.isExcluded = isExcluded;
   }



   /*******************************************************************************
    ** Fluent setter for isExcluded
    *******************************************************************************/
   public ApiProcessMetaData withIsExcluded(Boolean isExcluded)
   {
      this.isExcluded = isExcluded;
      return (this);
   }



   /*******************************************************************************
    ** Getter for method
    *******************************************************************************/
   public HttpMethod getMethod()
   {
      return (this.method);
   }



   /*******************************************************************************
    ** Setter for method
    *******************************************************************************/
   public void setMethod(HttpMethod method)
   {
      this.method = method;
   }



   /*******************************************************************************
    ** Fluent setter for method
    *******************************************************************************/
   public ApiProcessMetaData withMethod(HttpMethod method)
   {
      this.method = method;
      return (this);
   }



   /*******************************************************************************
    ** Getter for path
    *******************************************************************************/
   public String getPath()
   {
      return (this.path);
   }



   /*******************************************************************************
    ** Setter for path
    *******************************************************************************/
   public void setPath(String path)
   {
      this.path = path;
   }



   /*******************************************************************************
    ** Fluent setter for path
    *******************************************************************************/
   public ApiProcessMetaData withPath(String path)
   {
      this.path = path;
      return (this);
   }



   /*******************************************************************************
    ** Getter for customizers
    *******************************************************************************/
   public Map<String, QCodeReference> getCustomizers()
   {
      return (this.customizers);
   }



   /*******************************************************************************
    ** Setter for customizers
    *******************************************************************************/
   public void setCustomizers(Map<String, QCodeReference> customizers)
   {
      this.customizers = customizers;
   }



   /*******************************************************************************
    ** Fluent setter for customizers
    *******************************************************************************/
   public ApiProcessMetaData withCustomizers(Map<String, QCodeReference> customizers)
   {
      this.customizers = customizers;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ApiProcessMetaData withCustomizer(String role, QCodeReference customizer)
   {
      if(this.customizers == null)
      {
         this.customizers = new HashMap<>();
      }

      if(this.customizers.containsKey(role))
      {
         throw (new IllegalArgumentException("Attempt to add a second customizer with role [" + role + "] to apiProcess [" + apiProcessName + "]."));
      }
      this.customizers.put(role, customizer);
      return (this);
   }



   /*******************************************************************************
    ** Getter for output
    *******************************************************************************/
   public ApiProcessOutputInterface getOutput()
   {
      return (this.output);
   }



   /*******************************************************************************
    ** Setter for output
    *******************************************************************************/
   public void setOutput(ApiProcessOutputInterface output)
   {
      this.output = output;
   }



   /*******************************************************************************
    ** Fluent setter for output
    *******************************************************************************/
   public ApiProcessMetaData withOutput(ApiProcessOutputInterface output)
   {
      this.output = output;
      return (this);
   }



   /*******************************************************************************
    ** Getter for input
    *******************************************************************************/
   public ApiProcessInput getInput()
   {
      return (this.input);
   }



   /*******************************************************************************
    ** Setter for input
    *******************************************************************************/
   public void setInput(ApiProcessInput input)
   {
      this.input = input;
   }



   /*******************************************************************************
    ** Fluent setter for input
    *******************************************************************************/
   public ApiProcessMetaData withInput(ApiProcessInput input)
   {
      this.input = input;
      return (this);
   }



   /*******************************************************************************
    ** Getter for summary
    *******************************************************************************/
   public String getSummary()
   {
      return (this.summary);
   }



   /*******************************************************************************
    ** Setter for summary
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    ** Fluent setter for summary
    *******************************************************************************/
   public ApiProcessMetaData withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for description
    *******************************************************************************/
   public String getDescription()
   {
      return (this.description);
   }



   /*******************************************************************************
    ** Setter for description
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** Fluent setter for description
    *******************************************************************************/
   public ApiProcessMetaData withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void validate(QInstance qInstance, QProcessMetaData process, QInstanceValidator qInstanceValidator, String apiName)
   {
      if(BooleanUtils.isTrue(getIsExcluded()))
      {
         /////////////////////////////////////////////////
         // no validation needed for excluded processes //
         /////////////////////////////////////////////////
         return;
      }

      qInstanceValidator.assertCondition(getMethod() != null, "Missing a method for api process meta data for process: " + process.getName() + ", apiName: " + apiName);
   }



   /*******************************************************************************
    ** Getter for asyncMode
    *******************************************************************************/
   public AsyncMode getAsyncMode()
   {
      return (this.asyncMode);
   }



   /*******************************************************************************
    ** Setter for asyncMode
    *******************************************************************************/
   public void setAsyncMode(AsyncMode asyncMode)
   {
      this.asyncMode = asyncMode;
   }



   /*******************************************************************************
    ** Fluent setter for asyncMode
    *******************************************************************************/
   public ApiProcessMetaData withAsyncMode(AsyncMode asyncMode)
   {
      this.asyncMode = asyncMode;
      return (this);
   }



   /*******************************************************************************
    ** Getter for overrideProcessIsHidden
    *******************************************************************************/
   public Boolean getOverrideProcessIsHidden()
   {
      return (this.overrideProcessIsHidden);
   }



   /*******************************************************************************
    ** Setter for overrideProcessIsHidden
    *******************************************************************************/
   public void setOverrideProcessIsHidden(Boolean overrideProcessIsHidden)
   {
      this.overrideProcessIsHidden = overrideProcessIsHidden;
   }



   /*******************************************************************************
    ** Fluent setter for overrideProcessIsHidden
    *******************************************************************************/
   public ApiProcessMetaData withOverrideProcessIsHidden(Boolean overrideProcessIsHidden)
   {
      this.overrideProcessIsHidden = overrideProcessIsHidden;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tag
    *******************************************************************************/
   public String getTag()
   {
      return (this.tag);
   }



   /*******************************************************************************
    ** Setter for tag
    *******************************************************************************/
   public void setTag(String tag)
   {
      this.tag = tag;
   }



   /*******************************************************************************
    ** Fluent setter for tag
    *******************************************************************************/
   public ApiProcessMetaData withTag(String tag)
   {
      this.tag = tag;
      return (this);
   }

}
