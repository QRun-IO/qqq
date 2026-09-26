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

package com.kingsrook.qqq.backend.core.model.metadata.security;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/*******************************************************************************
 ** Define (for a table) a lock that applies to records in the table - e.g.,
 ** a key type, and a field that has values for that key.
 **
 ** Here's an example of how the joinNameChain should be set up:
 ** given a table:  orderLineItemExtrinsic (that's 2 away from order, where the security field is):
 ** - recordSecurityLock.fieldName = order.clientId
 ** - recordSecurityLock.joinNameChain = [orderJoinOrderLineItem, orderLineItemJoinOrderLineItemExtrinsic]
 ** that is - what's the chain that takes us FROM the security fieldName TO the table with the lock.
 **
 ** LockScope controls what the lock prevents users from doing without a valid key.
 ** - READ_AND_WRITE means that users cannot read or write records without a valid key.
 ** - WRITE means that users cannot write records without a valid key (but they can read them).
 *******************************************************************************/
public class RecordSecurityLock implements Cloneable
{
   private String            securityKeyType;
   private String            fieldName;
   private List<String>      joinNameChain;
   private NullValueBehavior nullValueBehavior = NullValueBehavior.DENY;

   private LockScope lockScope = LockScope.READ_AND_WRITE;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RecordSecurityLock clone()
   {
      try
      {
         RecordSecurityLock clone = (RecordSecurityLock) super.clone();

         /////////////////////////
         // deep-clone the list //
         /////////////////////////
         if(joinNameChain != null)
         {
            clone.joinNameChain = new ArrayList<>();
            clone.joinNameChain.addAll(joinNameChain);
         }

         return (clone);
      }
      catch(CloneNotSupportedException e)
      {
         throw (new RuntimeException("Could not clone", e));
      }
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public RecordSecurityLock()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public enum NullValueBehavior
   {
      ALLOW,
      ALLOW_WRITE_ONLY, // not common - but see Audit, where you can do a thing that inserts them into a generic table, even though you can't later read them yourself...
      DENY;


      ////////////////////////////////////////////////////////////////////
      // for use in tryToGetFromString, where we'll lowercase the input //
      ////////////////////////////////////////////////////////////////////
      private static final Map<String, NullValueBehavior> stringMapping = new HashMap<>();

      static
      {
         stringMapping.put("allow", ALLOW);
         stringMapping.put("allow_write_only", ALLOW_WRITE_ONLY);
         stringMapping.put("allowwriteonly", ALLOW_WRITE_ONLY);
         stringMapping.put("deny", DENY);
      }

      /*******************************************************************************
       **
       *******************************************************************************/
      public static NullValueBehavior tryToGetFromString(String string)
      {
         if(string == null)
         {
            return (null);
         }

         return stringMapping.get(string.toLowerCase());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public enum LockScope
   {
      READ_AND_WRITE, // lock both reads and writes
      WRITE, // only lock writes
      READ // only lock reads
   }



   /*******************************************************************************
    ** Getter for securityKeyType
    *******************************************************************************/
   public String getSecurityKeyType()
   {
      return (this.securityKeyType);
   }



   /*******************************************************************************
    ** Setter for securityKeyType
    *******************************************************************************/
   public void setSecurityKeyType(String securityKeyType)
   {
      this.securityKeyType = securityKeyType;
   }



   /*******************************************************************************
    ** Fluent setter for securityKeyType
    *******************************************************************************/
   public RecordSecurityLock withSecurityKeyType(String securityKeyType)
   {
      this.securityKeyType = securityKeyType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldName
    *******************************************************************************/
   public String getFieldName()
   {
      return (this.fieldName);
   }



   /*******************************************************************************
    ** Setter for fieldName
    *******************************************************************************/
   public void setFieldName(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    ** Fluent setter for fieldName
    *******************************************************************************/
   public RecordSecurityLock withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for nullValueBehavior
    *******************************************************************************/
   public NullValueBehavior getNullValueBehavior()
   {
      return (this.nullValueBehavior);
   }



   /*******************************************************************************
    ** Setter for nullValueBehavior
    *******************************************************************************/
   public void setNullValueBehavior(NullValueBehavior nullValueBehavior)
   {
      this.nullValueBehavior = nullValueBehavior;
   }



   /*******************************************************************************
    ** Fluent setter for nullValueBehavior
    *******************************************************************************/
   public RecordSecurityLock withNullValueBehavior(NullValueBehavior nullValueBehavior)
   {
      this.nullValueBehavior = nullValueBehavior;
      return (this);
   }



   /*******************************************************************************
    ** Getter for joinNameChain
    *******************************************************************************/
   public List<String> getJoinNameChain()
   {
      return (this.joinNameChain);
   }



   /*******************************************************************************
    ** Setter for joinNameChain
    *******************************************************************************/
   public void setJoinNameChain(List<String> joinNameChain)
   {
      this.joinNameChain = joinNameChain;
   }



   /*******************************************************************************
    ** Fluent setter for joinNameChain
    *******************************************************************************/
   public RecordSecurityLock withJoinNameChain(List<String> joinNameChain)
   {
      this.joinNameChain = joinNameChain;
      return (this);
   }



   /*******************************************************************************
    ** Getter for lockScope
    *******************************************************************************/
   public LockScope getLockScope()
   {
      return (this.lockScope);
   }



   /*******************************************************************************
    ** Setter for lockScope
    *******************************************************************************/
   public void setLockScope(LockScope lockScope)
   {
      this.lockScope = lockScope;
   }



   /*******************************************************************************
    ** Fluent setter for lockScope
    *******************************************************************************/
   public RecordSecurityLock withLockScope(LockScope lockScope)
   {
      this.lockScope = lockScope;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return "RecordSecurityLock{"
         + "securityKeyType='" + securityKeyType + '\''
         + ", fieldName='" + fieldName + '\''
         + ", joinNameChain=" + joinNameChain
         + ", nullValueBehavior=" + nullValueBehavior
         + ", lockScope=" + lockScope
         + '}';
   }

}

