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

package com.kingsrook.qqq.backend.core.model.metadata.processes;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.instances.QInstanceHelpContentManager;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpRole;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;


/*******************************************************************************
 ** Meta-Data to define a front-end step in a process in a QQQ instance (e.g.,
 ** a screen presented to a user).
 **
 *******************************************************************************/
public class QFrontendStepMetaData extends QStepMetaData implements Cloneable
{
   private List<QFrontendComponentMetaData> components;
   private List<QFieldMetaData>             formFields;
   private List<QFieldMetaData>             viewFields;
   private List<QFieldMetaData>             recordListFields;
   private Map<String, QFieldMetaData>      formFieldMap;

   private String format;
   private String backStepName;

   private List<QHelpContent> helpContents;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFrontendStepMetaData()
   {
      setStepType("frontend");
   }



   /*******************************************************************************
    ** Getter for components
    **
    *******************************************************************************/
   public List<QFrontendComponentMetaData> getComponents()
   {
      return components;
   }



   /*******************************************************************************
    ** Setter for components
    **
    *******************************************************************************/
   public void setComponents(List<QFrontendComponentMetaData> components)
   {
      this.components = components;
   }



   /*******************************************************************************
    ** Fluent setter for adding 1 component
    **
    *******************************************************************************/
   public QFrontendStepMetaData withComponent(QFrontendComponentMetaData component)
   {
      if(this.components == null)
      {
         this.components = new ArrayList<>();
      }
      this.components.add(component);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for components
    **
    *******************************************************************************/
   public QFrontendStepMetaData withComponents(List<QFrontendComponentMetaData> components)
   {
      this.components = components;
      return (this);
   }



   /*******************************************************************************
    ** Getter for a single formFields by its name
    **
    *******************************************************************************/
   public QFieldMetaData getFormField(String fieldName)
   {
      if(formFieldMap != null && formFieldMap.containsKey(fieldName))
      {
         return (formFieldMap.get(fieldName));
      }

      return (null);
   }



   /*******************************************************************************
    ** Getter for formFields
    **
    *******************************************************************************/
   public List<QFieldMetaData> getFormFields()
   {
      return formFields;
   }



   /*******************************************************************************
    ** adder for formFields
    **
    *******************************************************************************/
   public void addFormField(QFieldMetaData fieldMetaData)
   {
      if(fieldMetaData != null)
      {
         if(formFieldMap == null)
         {
            formFieldMap = new HashMap<>();
         }
         if(formFields == null)
         {
            formFields = new ArrayList<>();
         }

         formFieldMap.put(fieldMetaData.getName(), fieldMetaData);
         formFields.add(fieldMetaData);
      }
   }



   /*******************************************************************************
    ** Setter for formFields
    **
    *******************************************************************************/
   public void setFormFields(List<QFieldMetaData> formFields)
   {
      if(formFields == null)
      {
         this.formFields = null;
         this.formFieldMap = null;
      }
      else
      {
         for(QFieldMetaData fieldMetaData : formFields)
         {
            addFormField(fieldMetaData);
         }
      }
   }



   /*******************************************************************************
    ** fluent setter to add a single form field
    **
    *******************************************************************************/
   public QFrontendStepMetaData withFormField(QFieldMetaData formField)
   {
      addFormField(formField);
      return (this);
   }



   /*******************************************************************************
    ** fluent setter for formFields
    **
    *******************************************************************************/
   public QFrontendStepMetaData withFormFields(List<QFieldMetaData> formFields)
   {
      this.setFormFields(formFields);
      return (this);
   }



   /*******************************************************************************
    ** Getter for viewFields
    **
    *******************************************************************************/
   public List<QFieldMetaData> getViewFields()
   {
      return viewFields;
   }



   /*******************************************************************************
    ** Setter for viewFields
    **
    *******************************************************************************/
   public void setViewFields(List<QFieldMetaData> viewFields)
   {
      this.viewFields = viewFields;
   }



   /*******************************************************************************
    ** fluent setter to add a single view field
    **
    *******************************************************************************/
   public QFrontendStepMetaData withViewField(QFieldMetaData viewField)
   {
      if(this.viewFields == null)
      {
         this.viewFields = new ArrayList<>();
      }
      this.viewFields.add(viewField);
      return (this);
   }



   /*******************************************************************************
    ** fluent setter for viewFields
    **
    *******************************************************************************/
   public QFrontendStepMetaData withViewFields(List<QFieldMetaData> viewFields)
   {
      this.viewFields = viewFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordListFields
    **
    *******************************************************************************/
   public List<QFieldMetaData> getRecordListFields()
   {
      return recordListFields;
   }



   /*******************************************************************************
    ** Setter for recordListFields
    **
    *******************************************************************************/
   public void setRecordListFields(List<QFieldMetaData> recordListFields)
   {
      this.recordListFields = recordListFields;
   }



   /*******************************************************************************
    ** fluent setter to add a single recordList field
    **
    *******************************************************************************/
   public QFrontendStepMetaData withRecordListField(QFieldMetaData recordListField)
   {
      if(this.recordListFields == null)
      {
         this.recordListFields = new ArrayList<>();
      }
      this.recordListFields.add(recordListField);
      return (this);
   }



   /*******************************************************************************
    ** fluent setter for recordListFields
    **
    *******************************************************************************/
   public QFrontendStepMetaData withRecordListFields(List<QFieldMetaData> recordListFields)
   {
      this.recordListFields = recordListFields;
      return (this);
   }



   /*******************************************************************************
    ** fluent setter for name
    **
    *******************************************************************************/
   @Override
   public QFrontendStepMetaData withName(String name)
   {
      setName(name);
      return (this);
   }



   /*******************************************************************************
    ** fluent setter for label
    **
    *******************************************************************************/
   @Override
   public QFrontendStepMetaData withLabel(String label)
   {
      setLabel(label);
      return (this);
   }



   /*******************************************************************************
    ** Get a list of all of the input fields used by this function
    *******************************************************************************/
   @JsonIgnore // because this is a computed property - we don't want it in our json.
   @Override
   public List<QFieldMetaData> getInputFields()
   {
      List<QFieldMetaData> rs = new ArrayList<>();

      if(formFields != null)
      {
         rs.addAll(formFields);
      }

      return (rs);
   }



   /*******************************************************************************
    ** Getter for helpContents
    *******************************************************************************/
   public List<QHelpContent> getHelpContents()
   {
      return (this.helpContents);
   }



   /*******************************************************************************
    ** Setter for helpContents
    *******************************************************************************/
   public void setHelpContents(List<QHelpContent> helpContents)
   {
      this.helpContents = helpContents;
   }



   /*******************************************************************************
    ** Fluent setter for helpContents
    *******************************************************************************/
   public QFrontendStepMetaData withHelpContents(List<QHelpContent> helpContents)
   {
      this.helpContents = helpContents;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for adding 1 helpContent
    *******************************************************************************/
   public QFrontendStepMetaData withHelpContent(QHelpContent helpContent)
   {
      if(this.helpContents == null)
      {
         this.helpContents = new ArrayList<>();
      }

      QInstanceHelpContentManager.putHelpContentInList(helpContent, this.helpContents);
      return (this);
   }



   /*******************************************************************************
    ** remove a single helpContent based on its set of roles
    *******************************************************************************/
   public void removeHelpContent(Set<HelpRole> roles)
   {
      QInstanceHelpContentManager.removeHelpContentByRoleSetFromList(roles, this.helpContents);
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
   public QFrontendStepMetaData withFormat(String format)
   {
      this.format = format;
      return (this);
   }



   /*******************************************************************************
    ** Getter for backStepName
    *******************************************************************************/
   public String getBackStepName()
   {
      return (this.backStepName);
   }



   /*******************************************************************************
    ** Setter for backStepName
    *******************************************************************************/
   public void setBackStepName(String backStepName)
   {
      this.backStepName = backStepName;
   }



   /*******************************************************************************
    ** Fluent setter for backStepName
    *******************************************************************************/
   public QFrontendStepMetaData withBackStepName(String backStepName)
   {
      this.backStepName = backStepName;
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public QFrontendStepMetaData clone()
   {
      QFrontendStepMetaData clone = (QFrontendStepMetaData) super.clone();

      if(components != null)
      {
         clone.components = new ArrayList<>();
         for(QFrontendComponentMetaData component : components)
         {
            clone.components.add(component.clone());
         }
      }

      if(formFields != null)
      {
         clone.formFields = new ArrayList<>();
         for(QFieldMetaData formField : formFields)
         {
            clone.formFields.add(formField.clone());
         }
      }

      if(viewFields != null)
      {
         clone.viewFields = new ArrayList<>();
         for(QFieldMetaData viewField : viewFields)
         {
            clone.viewFields.add(viewField.clone());
         }
      }

      if(recordListFields != null)
      {
         clone.recordListFields = new ArrayList<>();
         for(QFieldMetaData formField : recordListFields)
         {
            clone.recordListFields.add(formField.clone());
         }
      }

      if(formFieldMap != null)
      {
         clone.formFieldMap = new HashMap<>();
         for(Map.Entry<String, QFieldMetaData> entry : formFieldMap.entrySet())
         {
            clone.formFieldMap.put(entry.getKey(), entry.getValue().clone());
         }
      }

      if(helpContents != null)
      {
         clone.helpContents = new ArrayList<>();
         for(QHelpContent helpContent : helpContents)
         {
            clone.helpContents.add(helpContent.clone());
         }
      }

      return clone;
   }

}
