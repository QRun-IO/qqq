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

package com.kingsrook.qqq.backend.module.mongodb.model.metadata;


import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.BackendFieldFunctionAdapterInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.BackendFieldFunctionAdapterRegistry;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionTypeIdentifier;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateTimeFunction;
import com.kingsrook.qqq.backend.module.mongodb.MongoDBBackendModule;
import com.kingsrook.qqq.backend.module.mongodb.fieldfunctions.MongoDBFieldFunctionAdapterInterface;
import com.kingsrook.qqq.backend.module.mongodb.fieldfunctions.MongoDBStringLengthFunction;
import com.kingsrook.qqq.backend.module.mongodb.fieldfunctions.MongoDBSubStringFunction;
import com.kingsrook.qqq.backend.module.mongodb.fieldfunctions.MongoDBWeekdayOfDateFunction;
import com.kingsrook.qqq.backend.module.mongodb.fieldfunctions.MongoDBWeekdayOfDateTimeFunction;


/*******************************************************************************
 ** Meta-data to provide details of a MongoDB backend (e.g., connection params)
 *******************************************************************************/
public class MongoDBBackendMetaData extends QBackendMetaData
{
   private String  host;
   private Integer port;
   private String  databaseName;
   private String  username;
   private String  password;
   private String  authSourceDatabase;
   private String  urlSuffix;

   private boolean transactionsSupported = true;

   private BackendFieldFunctionAdapterRegistry backendFieldFunctionAdapterRegistry = new BackendFieldFunctionAdapterRegistry();



   /*******************************************************************************
    ** Default Constructor.
    *******************************************************************************/
   public MongoDBBackendMetaData()
   {
      super();
      setBackendType(MongoDBBackendModule.class);
   }



   /*******************************************************************************
    ** Fluent setter, override to help fluent flows
    *******************************************************************************/
   @Override
   public MongoDBBackendMetaData withName(String name)
   {
      setName(name);
      return this;
   }



   /*******************************************************************************
    ** Getter for host
    **
    *******************************************************************************/
   public String getHost()
   {
      return host;
   }



   /*******************************************************************************
    ** Setter for host
    **
    *******************************************************************************/
   public void setHost(String host)
   {
      this.host = host;
   }



   /*******************************************************************************
    ** Fluent Setter for host
    **
    *******************************************************************************/
   public MongoDBBackendMetaData withHost(String host)
   {
      this.host = host;
      return (this);
   }



   /*******************************************************************************
    ** Getter for port
    **
    *******************************************************************************/
   public Integer getPort()
   {
      return port;
   }



   /*******************************************************************************
    ** Setter for port
    **
    *******************************************************************************/
   public void setPort(Integer port)
   {
      this.port = port;
   }



   /*******************************************************************************
    ** Fluent Setter for port
    **
    *******************************************************************************/
   public MongoDBBackendMetaData withPort(Integer port)
   {
      this.port = port;
      return (this);
   }



   /*******************************************************************************
    ** Getter for username
    **
    *******************************************************************************/
   public String getUsername()
   {
      return username;
   }



   /*******************************************************************************
    ** Setter for username
    **
    *******************************************************************************/
   public void setUsername(String username)
   {
      this.username = username;
   }



   /*******************************************************************************
    ** Fluent Setter for username
    **
    *******************************************************************************/
   public MongoDBBackendMetaData withUsername(String username)
   {
      this.username = username;
      return (this);
   }



   /*******************************************************************************
    ** Getter for password
    **
    *******************************************************************************/
   public String getPassword()
   {
      return password;
   }



   /*******************************************************************************
    ** Setter for password
    **
    *******************************************************************************/
   public void setPassword(String password)
   {
      this.password = password;
   }



   /*******************************************************************************
    ** Fluent Setter for password
    **
    *******************************************************************************/
   public MongoDBBackendMetaData withPassword(String password)
   {
      this.password = password;
      return (this);
   }



   /*******************************************************************************
    ** Called by the QInstanceEnricher - to do backend-type-specific enrichments.
    ** Original use case is:  reading secrets into fields (e.g., passwords).
    *******************************************************************************/
   @Override
   public void enrich()
   {
      super.enrich();
      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();
      username = interpreter.interpret(username);
      password = interpreter.interpret(password);

      doRegisterFieldFunctionAdapters();
   }



   /*******************************************************************************
    ** Register BackendFieldFunctionAdapterInterface classes to be used
    ** in this backend for known FieldFunctionTypes.
    **
    ** If a subclass needs different adapters, it can override this method,
    ** should call super to register the base functions first, then register
    ** its own override implementations via registerBackendFieldFunctionAdapter.
    *******************************************************************************/
   public void doRegisterFieldFunctionAdapters()
   {
      registerBackendFieldFunctionAdapter(StringLengthFunction.IDENTIFIER, MongoDBStringLengthFunction.class);
      registerBackendFieldFunctionAdapter(SubStringFunction.IDENTIFIER, MongoDBSubStringFunction.class);
      registerBackendFieldFunctionAdapter(WeekdayOfDateFunction.IDENTIFIER, MongoDBWeekdayOfDateFunction.class);
      registerBackendFieldFunctionAdapter(WeekdayOfDateTimeFunction.IDENTIFIER, MongoDBWeekdayOfDateTimeFunction.class);
   }



   /*******************************************************************************
    ** Register a BackendFieldFunctionAdapterInterface class to be used
    ** in this backend for a specific FieldFunctionType.
    *******************************************************************************/
   protected void registerBackendFieldFunctionAdapter(FieldFunctionTypeIdentifier fieldFunctionTypeIdentifier, Class<? extends MongoDBFieldFunctionAdapterInterface> adapterClass)
   {
      backendFieldFunctionAdapterRegistry.register(fieldFunctionTypeIdentifier, getBackendType(), new QCodeReference(adapterClass));
   }



   /*******************************************************************************
    ** Get an instance of a MongoDBFieldFunctionAdapterInterface for a
    ** specific FieldFunctionType.
    *******************************************************************************/
   public MongoDBFieldFunctionAdapterInterface getFieldFunctionAdapter(FieldFunctionTypeIdentifier fieldFunctionTypeIdentifier)
   {
      BackendFieldFunctionAdapterInterface fieldFunctionAdapter = backendFieldFunctionAdapterRegistry.getFieldFunctionAdapter(fieldFunctionTypeIdentifier);
      if(fieldFunctionAdapter != null)
      {
         if(fieldFunctionAdapter instanceof MongoDBFieldFunctionAdapterInterface mongoDBAdapter)
         {
            return mongoDBAdapter;
         }
         else
         {
            throw new QRuntimeException("The registered adapter does not implement the MongoDBFieldFunctionAdapterInterface (adapterType=" + fieldFunctionAdapter.getClass().getName() + ")");
         }
      }
      return (null);
   }



   /*******************************************************************************
    ** Getter for urlSuffix
    *******************************************************************************/
   public String getUrlSuffix()
   {
      return (this.urlSuffix);
   }



   /*******************************************************************************
    ** Setter for urlSuffix
    *******************************************************************************/
   public void setUrlSuffix(String urlSuffix)
   {
      this.urlSuffix = urlSuffix;
   }



   /*******************************************************************************
    ** Fluent setter for urlSuffix
    *******************************************************************************/
   public MongoDBBackendMetaData withUrlSuffix(String urlSuffix)
   {
      this.urlSuffix = urlSuffix;
      return (this);
   }



   /*******************************************************************************
    ** Getter for databaseName
    *******************************************************************************/
   public String getDatabaseName()
   {
      return (this.databaseName);
   }



   /*******************************************************************************
    ** Setter for databaseName
    *******************************************************************************/
   public void setDatabaseName(String databaseName)
   {
      this.databaseName = databaseName;
   }



   /*******************************************************************************
    ** Fluent setter for databaseName
    *******************************************************************************/
   public MongoDBBackendMetaData withDatabaseName(String databaseName)
   {
      this.databaseName = databaseName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for transactionsSupported
    *******************************************************************************/
   public boolean getTransactionsSupported()
   {
      return (this.transactionsSupported);
   }



   /*******************************************************************************
    ** Setter for transactionsSupported
    *******************************************************************************/
   public void setTransactionsSupported(boolean transactionsSupported)
   {
      this.transactionsSupported = transactionsSupported;
   }



   /*******************************************************************************
    ** Fluent setter for transactionsSupported
    *******************************************************************************/
   public MongoDBBackendMetaData withTransactionsSupported(boolean transactionsSupported)
   {
      this.transactionsSupported = transactionsSupported;
      return (this);
   }



   /*******************************************************************************
    ** Getter for authSourceDatabase
    *******************************************************************************/
   public String getAuthSourceDatabase()
   {
      return (this.authSourceDatabase);
   }



   /*******************************************************************************
    ** Setter for authSourceDatabase
    *******************************************************************************/
   public void setAuthSourceDatabase(String authSourceDatabase)
   {
      this.authSourceDatabase = authSourceDatabase;
   }



   /*******************************************************************************
    ** Fluent setter for authSourceDatabase
    *******************************************************************************/
   public MongoDBBackendMetaData withAuthSourceDatabase(String authSourceDatabase)
   {
      this.authSourceDatabase = authSourceDatabase;
      return (this);
   }

}
