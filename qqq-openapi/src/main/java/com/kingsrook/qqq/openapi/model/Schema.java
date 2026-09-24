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

package com.kingsrook.qqq.openapi.model;


import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonGetter;


/*******************************************************************************
 **
 *******************************************************************************/
public class Schema
{
   private String               type;
   private String               format;
   private String               description;
   private List<String>         enumValues;
   private Schema               items;
   private Map<String, Schema>  properties;
   private Object               example;
   private Map<String, Example> examples;
   private String               ref;
   private List<Schema>         allOf;
   private List<Schema>         anyOf;
   private List<Schema>         oneOf;
   private Boolean              readOnly;
   private Boolean              nullable;
   private Integer              maxLength;
   private Discriminator        discriminator;
   private Object               additionalProperties;



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public String getType()
   {
      return (this.type);
   }



   /*******************************************************************************
    ** Setter for type
    *******************************************************************************/
   public void setType(Type type)
   {
      this.type = type == null ? null : type.toString().toLowerCase();
   }



   /*******************************************************************************
    ** Fluent setter for type
    *******************************************************************************/
   public Schema withType(Type type)
   {
      setType(type);
      return (this);
   }



   /*******************************************************************************
    ** Getter for format
    *******************************************************************************/
   public String getFormat()
   {
      return (this.format);
   }



   /*******************************************************************************
    ** Setter for format
    *******************************************************************************/
   public void setFormat(String format)
   {
      this.format = format;
   }



   /*******************************************************************************
    ** Fluent setter for format
    *******************************************************************************/
   public Schema withFormat(String format)
   {
      this.format = format;
      return (this);
   }



   /*******************************************************************************
    ** Getter for items
    *******************************************************************************/
   public Schema getItems()
   {
      return (this.items);
   }



   /*******************************************************************************
    ** Setter for items
    *******************************************************************************/
   public void setItems(Schema items)
   {
      this.items = items;
   }



   /*******************************************************************************
    ** Fluent setter for items
    *******************************************************************************/
   public Schema withItems(Schema items)
   {
      this.items = items;
      return (this);
   }



   /*******************************************************************************
    ** Getter for properties
    *******************************************************************************/
   public Map<String, Schema> getProperties()
   {
      return (this.properties);
   }



   /*******************************************************************************
    ** Setter for properties
    *******************************************************************************/
   public void setProperties(Map<String, Schema> properties)
   {
      this.properties = properties;
   }



   /*******************************************************************************
    ** Fluent setter for properties
    *******************************************************************************/
   public Schema withProperties(Map<String, Schema> properties)
   {
      this.properties = properties;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for properties
    *******************************************************************************/
   public Schema withProperty(String key, Schema schema)
   {
      if(this.properties == null)
      {
         this.properties = new LinkedHashMap<>();
      }
      this.properties.put(key, schema);
      return (this);
   }



   /*******************************************************************************
    ** Getter for example
    *******************************************************************************/
   public Object getExample()
   {
      return (this.example);
   }



   /*******************************************************************************
    ** Setter for example
    *******************************************************************************/
   public void setExample(String example)
   {
      this.example = example;
   }



   /*******************************************************************************
    ** Setter for example
    *******************************************************************************/
   public void setExample(BigDecimal example)
   {
      this.example = example;
   }



   /*******************************************************************************
    ** Fluent setter for example
    *******************************************************************************/
   public Schema withExample(Object example)
   {
      this.example = example;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for example
    *******************************************************************************/
   public Schema withExample(String example)
   {
      this.example = example;
      return (this);
   }



   /*******************************************************************************
    ** Setter for example
    *******************************************************************************/
   public void setExample(List<?> example)
   {
      this.example = example;
   }



   /*******************************************************************************
    ** Fluent setter for example
    *******************************************************************************/
   public Schema withExample(List<?> example)
   {
      this.example = example;
      return (this);
   }



   /*******************************************************************************
    ** Getter for ref
    *******************************************************************************/
   @JsonGetter("$ref")
   public String getRef()
   {
      return (this.ref);
   }



   /*******************************************************************************
    ** Setter for ref
    *******************************************************************************/
   public void setRef(String ref)
   {
      this.ref = ref;
   }



   /*******************************************************************************
    ** Fluent setter for ref
    *******************************************************************************/
   public Schema withRef(String ref)
   {
      this.ref = ref;
      return (this);
   }



   /*******************************************************************************
    ** Getter for allOf
    *******************************************************************************/
   public List<Schema> getAllOf()
   {
      return (this.allOf);
   }



   /*******************************************************************************
    ** Setter for allOf
    *******************************************************************************/
   public void setAllOf(List<Schema> allOf)
   {
      this.allOf = allOf;
   }



   /*******************************************************************************
    ** Fluent setter for allOf
    *******************************************************************************/
   public Schema withAllOf(List<Schema> allOf)
   {
      this.allOf = allOf;
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
   public Schema withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for enumValues
    *******************************************************************************/
   @JsonGetter("enum")
   public List<String> getEnumValues()
   {
      return (this.enumValues);
   }



   /*******************************************************************************
    ** Setter for enumValues
    *******************************************************************************/
   public void setEnumValues(List<String> enumValues)
   {
      this.enumValues = enumValues;
   }



   /*******************************************************************************
    ** Fluent setter for enumValues
    *******************************************************************************/
   public Schema withEnumValues(List<String> enumValues)
   {
      this.enumValues = enumValues;
      return (this);
   }



   /*******************************************************************************
    ** Getter for readOnly
    *******************************************************************************/
   public Boolean getReadOnly()
   {
      return (this.readOnly);
   }



   /*******************************************************************************
    ** Setter for readOnly
    *******************************************************************************/
   public void setReadOnly(Boolean readOnly)
   {
      this.readOnly = readOnly;
   }



   /*******************************************************************************
    ** Fluent setter for readOnly
    *******************************************************************************/
   public Schema withReadOnly(Boolean readOnly)
   {
      this.readOnly = readOnly;
      return (this);
   }



   /*******************************************************************************
    ** Getter for nullable
    *******************************************************************************/
   public Boolean getNullable()
   {
      return (this.nullable);
   }



   /*******************************************************************************
    ** Setter for nullable
    *******************************************************************************/
   public void setNullable(Boolean nullable)
   {
      this.nullable = nullable;
   }



   /*******************************************************************************
    ** Fluent setter for nullable
    *******************************************************************************/
   public Schema withNullable(Boolean nullable)
   {
      this.nullable = nullable;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxLength
    *******************************************************************************/
   public Integer getMaxLength()
   {
      return (this.maxLength);
   }



   /*******************************************************************************
    ** Setter for maxLength
    *******************************************************************************/
   public void setMaxLength(Integer maxLength)
   {
      this.maxLength = maxLength;
   }



   /*******************************************************************************
    ** Fluent setter for maxLength
    *******************************************************************************/
   public Schema withMaxLength(Integer maxLength)
   {
      this.maxLength = maxLength;
      return (this);
   }



   /*******************************************************************************
    ** Getter for discriminator
    *******************************************************************************/
   public Discriminator getDiscriminator()
   {
      return (this.discriminator);
   }



   /*******************************************************************************
    ** Setter for discriminator
    *******************************************************************************/
   public void setDiscriminator(Discriminator discriminator)
   {
      this.discriminator = discriminator;
   }



   /*******************************************************************************
    ** Fluent setter for discriminator
    *******************************************************************************/
   public Schema withDiscriminator(Discriminator discriminator)
   {
      this.discriminator = discriminator;
      return (this);
   }



   /*******************************************************************************
    ** Getter for anyOf
    *******************************************************************************/
   public List<Schema> getAnyOf()
   {
      return (this.anyOf);
   }



   /*******************************************************************************
    ** Setter for anyOf
    *******************************************************************************/
   public void setAnyOf(List<Schema> anyOf)
   {
      this.anyOf = anyOf;
   }



   /*******************************************************************************
    ** Fluent setter for anyOf
    *******************************************************************************/
   public Schema withAnyOf(List<Schema> anyOf)
   {
      this.anyOf = anyOf;
      return (this);
   }



   /*******************************************************************************
    ** Getter for oneOf
    *******************************************************************************/
   public List<Schema> getOneOf()
   {
      return (this.oneOf);
   }



   /*******************************************************************************
    ** Setter for oneOf
    *******************************************************************************/
   public void setOneOf(List<Schema> oneOf)
   {
      this.oneOf = oneOf;
   }



   /*******************************************************************************
    ** Fluent setter for oneOf
    *******************************************************************************/
   public Schema withOneOf(List<Schema> oneOf)
   {
      this.oneOf = oneOf;
      return (this);
   }



   /*******************************************************************************
    ** Getter for examples
    *******************************************************************************/
   public Map<String, Example> getExamples()
   {
      return (this.examples);
   }



   /*******************************************************************************
    ** Setter for examples
    *******************************************************************************/
   public void setExamples(Map<String, Example> examples)
   {
      this.examples = examples;
   }



   /*******************************************************************************
    ** Fluent setter for examples
    *******************************************************************************/
   public Schema withExamples(Map<String, Example> examples)
   {
      this.examples = examples;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for examples
    *******************************************************************************/
   public Schema withExample(String name, Example example)
   {
      if(this.examples == null)
      {
         this.examples = new LinkedHashMap<>();
      }
      this.examples.put(name, example);
      return (this);
   }



   /*******************************************************************************
    ** Getter for additionalProperties
    *******************************************************************************/
   public Object getAdditionalProperties()
   {
      return (this.additionalProperties);
   }



   /*******************************************************************************
    ** Setter for additionalProperties
    *******************************************************************************/
   public void setAdditionalProperties(Schema additionalProperties)
   {
      this.additionalProperties = additionalProperties;
   }



   /*******************************************************************************
    ** Fluent setter for additionalProperties
    *******************************************************************************/
   public Schema withAdditionalProperties(Schema additionalProperties)
   {
      this.additionalProperties = additionalProperties;
      return (this);
   }



   /*******************************************************************************
    ** Setter for additionalProperties
    *******************************************************************************/
   public void setAdditionalProperties(Boolean additionalProperties)
   {
      this.additionalProperties = additionalProperties;
   }



   /*******************************************************************************
    ** Fluent setter for additionalProperties
    *******************************************************************************/
   public Schema withAdditionalProperties(Boolean additionalProperties)
   {
      this.additionalProperties = additionalProperties;
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public Schema withRefToSchema(String componentName)
   {
      return withRef("#/components/schemas/" + componentName);
   }

}
