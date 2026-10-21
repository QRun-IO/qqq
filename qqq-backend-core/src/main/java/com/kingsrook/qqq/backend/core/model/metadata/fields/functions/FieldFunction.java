/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.fields.functions;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.serialization.FieldFunctionDeserializer;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/***************************************************************************
 * Application of a Function to a field - e.g., to take its raw or pure value
 * as stored in the backend, and apply a function to it (as in the math sense),
 * to get out a different value.
 *
 * <p>Used in virtual fields and query criteria to transform or extract a derived
 * value from a raw field.</p>
 ***************************************************************************/
@JsonDeserialize(using = FieldFunctionDeserializer.class)
public class FieldFunction implements Serializable, Cloneable
{
   private static final QLogger LOG = QLogger.getLogger(FieldFunction.class);

   private String                      fieldName;
   private FieldFunctionTypeIdentifier functionTypeIdentifier;
   private Map<String, Serializable>   arguments;



   /***************************************************************************
    * Returns the argument value for the given parameter name, or the parameter's
    * declared default value if no argument was supplied.
    ***************************************************************************/
   public <C extends Serializable> C getArgumentValueOrDefault(Class<C> type, String name)
   {
      Serializable argumentValue = arguments == null ? null : arguments.get(name);
      if(argumentValue != null)
      {
         return (ValueUtils.getValueAsType(type, argumentValue));
      }

      FieldFunctionType fieldFunctionType = FieldFunctionTypeRegistry.ofOrWithNew(QContext.getQInstance()).getFieldFunctionType(getFunctionTypeIdentifier());
      if(fieldFunctionType == null)
      {
         LOG.warn("No fieldFunctionType is registered for the specified identifier", logPair("identifier", getFunctionTypeIdentifierName()));
         return (null);
      }

      if(fieldFunctionType.getParameters() == null)
      {
         return (null);
      }

      Optional<FieldFunctionParameter> parameter = fieldFunctionType.getParameters().stream().filter(p -> p.getName().equals(name)).findFirst();
      if(parameter.isPresent() && parameter.get().getDefaultValue() != null)
      {
         return (ValueUtils.getValueAsType(type, parameter.get().getDefaultValue()));
      }

      return (null);
   }



   /*******************************************************************************
    * Getter for fieldName
    * @see #withFieldName(String)
    *******************************************************************************/
   public String getFieldName()
   {
      return (this.fieldName);
   }



   /*******************************************************************************
    * Setter for fieldName
    * @see #withFieldName(String)
    *******************************************************************************/
   public void setFieldName(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    * Fluent setter for fieldName
    *
    * @param fieldName
    * The name of the field (on the record) to which this function should be applied.
    * @return this
    *******************************************************************************/
   public FieldFunction withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /***************************************************************************
    * Returns a deep clone of this FieldFunction, including a shallow copy of
    * the arguments map.
    ***************************************************************************/
   @Override
   public FieldFunction clone()
   {
      try
      {
         FieldFunction clone = (FieldFunction) super.clone();

         if(arguments != null)
         {
            clone.arguments = new HashMap<>();
            clone.arguments.putAll(arguments);
         }

         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }



   /*******************************************************************************
    * Getter for functionTypeIdentifier
    * @see #withFunctionTypeIdentifier(FieldFunctionTypeIdentifier)
    *
    * Note:  Marked as JSON Ignore - we'll just serialize the name via
    * {@link #getFunctionTypeIdentifierName()}} and deserialize it via
    * {@link FieldFunctionDeserializer}.
    *******************************************************************************/
   @JsonIgnore
   public FieldFunctionTypeIdentifier getFunctionTypeIdentifier()
   {
      return (this.functionTypeIdentifier);
   }



   /*******************************************************************************
    * just serialize the identifier name - not an object.
    * See {@link #getFunctionTypeIdentifier()}
    *******************************************************************************/
   @JsonProperty
   public String getFunctionTypeIdentifierName()
   {
      return (this.functionTypeIdentifier == null ? null : this.functionTypeIdentifier.getName());
   }



   /*******************************************************************************
    * Setter for functionTypeIdentifier
    * @see #withFunctionTypeIdentifier(FieldFunctionTypeIdentifier)
    *******************************************************************************/
   public void setFunctionTypeIdentifier(FieldFunctionTypeIdentifier functionTypeIdentifier)
   {
      this.functionTypeIdentifier = functionTypeIdentifier;
   }



   /*******************************************************************************
    * Fluent setter for functionTypeIdentifier
    *
    * @param functionTypeIdentifier
    * The identifier that specifies which function type should be applied to the field.
    * @return this
    *******************************************************************************/
   public FieldFunction withFunctionTypeIdentifier(FieldFunctionTypeIdentifier functionTypeIdentifier)
   {
      this.functionTypeIdentifier = functionTypeIdentifier;
      return (this);
   }



   /*******************************************************************************
    * Getter for arguments
    * @see #withArguments(Map)
    *******************************************************************************/
   public Map<String, Serializable> getArguments()
   {
      return (this.arguments);
   }



   /*******************************************************************************
    * Setter for arguments
    * @see #withArguments(Map)
    *******************************************************************************/
   public void setArguments(Map<String, Serializable> arguments)
   {
      this.arguments = arguments;
   }



   /*******************************************************************************
    * Fluent setter for arguments
    *
    * @param arguments
    * Map of argument names to their values (e.g., fromIndex=1, length=5),
    * interpreted by the function type.
    * @return this
    *******************************************************************************/
   public FieldFunction withArguments(Map<String, Serializable> arguments)
   {
      this.arguments = arguments;
      return (this);
   }

}



