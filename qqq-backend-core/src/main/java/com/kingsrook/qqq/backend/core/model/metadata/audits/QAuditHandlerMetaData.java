/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.audits;


import java.util.Set;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** Metadata for registering an audit handler in a QInstance.
 ** Handlers can be configured to run globally or for specific tables.
 *******************************************************************************/
public class QAuditHandlerMetaData implements TopLevelMetaDataInterface
{
   private String                    name;
   private QCodeReference            handlerCode;
   private AuditHandlerType          handlerType;
   private Boolean                   isAsync;
   private AuditHandlerFailurePolicy failurePolicy;
   private Set<String>               tableNames;
   private Boolean                   enabled;



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public QAuditHandlerMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Add self to the QInstance
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      qInstance.addAuditHandler(this);
   }



   /*******************************************************************************
    ** Getter for handlerCode
    *******************************************************************************/
   public QCodeReference getHandlerCode()
   {
      return (this.handlerCode);
   }



   /*******************************************************************************
    ** Setter for handlerCode
    *******************************************************************************/
   public void setHandlerCode(QCodeReference handlerCode)
   {
      this.handlerCode = handlerCode;
   }



   /*******************************************************************************
    ** Fluent setter for handlerCode
    *******************************************************************************/
   public QAuditHandlerMetaData withHandlerCode(QCodeReference handlerCode)
   {
      this.handlerCode = handlerCode;
      return (this);
   }



   /*******************************************************************************
    ** Getter for handlerType
    *******************************************************************************/
   public AuditHandlerType getHandlerType()
   {
      return (this.handlerType);
   }



   /*******************************************************************************
    ** Setter for handlerType
    *******************************************************************************/
   public void setHandlerType(AuditHandlerType handlerType)
   {
      this.handlerType = handlerType;
   }



   /*******************************************************************************
    ** Fluent setter for handlerType
    *******************************************************************************/
   public QAuditHandlerMetaData withHandlerType(AuditHandlerType handlerType)
   {
      this.handlerType = handlerType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isAsync
    *******************************************************************************/
   public Boolean getIsAsync()
   {
      return (this.isAsync);
   }



   /*******************************************************************************
    ** Setter for isAsync
    *******************************************************************************/
   public void setIsAsync(Boolean isAsync)
   {
      this.isAsync = isAsync;
   }



   /*******************************************************************************
    ** Fluent setter for isAsync
    *******************************************************************************/
   public QAuditHandlerMetaData withIsAsync(Boolean isAsync)
   {
      this.isAsync = isAsync;
      return (this);
   }



   /*******************************************************************************
    ** Getter for failurePolicy
    *******************************************************************************/
   public AuditHandlerFailurePolicy getFailurePolicy()
   {
      return (this.failurePolicy);
   }



   /*******************************************************************************
    ** Setter for failurePolicy
    *******************************************************************************/
   public void setFailurePolicy(AuditHandlerFailurePolicy failurePolicy)
   {
      this.failurePolicy = failurePolicy;
   }



   /*******************************************************************************
    ** Fluent setter for failurePolicy
    *******************************************************************************/
   public QAuditHandlerMetaData withFailurePolicy(AuditHandlerFailurePolicy failurePolicy)
   {
      this.failurePolicy = failurePolicy;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableNames
    *******************************************************************************/
   public Set<String> getTableNames()
   {
      return (this.tableNames);
   }



   /*******************************************************************************
    ** Setter for tableNames
    *******************************************************************************/
   public void setTableNames(Set<String> tableNames)
   {
      this.tableNames = tableNames;
   }



   /*******************************************************************************
    ** Fluent setter for tableNames
    *******************************************************************************/
   public QAuditHandlerMetaData withTableNames(Set<String> tableNames)
   {
      this.tableNames = tableNames;
      return (this);
   }



   /*******************************************************************************
    ** Getter for enabled
    *******************************************************************************/
   public Boolean getEnabled()
   {
      return (this.enabled);
   }



   /*******************************************************************************
    ** Setter for enabled
    *******************************************************************************/
   public void setEnabled(Boolean enabled)
   {
      this.enabled = enabled;
   }



   /*******************************************************************************
    ** Fluent setter for enabled
    *******************************************************************************/
   public QAuditHandlerMetaData withEnabled(Boolean enabled)
   {
      this.enabled = enabled;
      return (this);
   }

}
