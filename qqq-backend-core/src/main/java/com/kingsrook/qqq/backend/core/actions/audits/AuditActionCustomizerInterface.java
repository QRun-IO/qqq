/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.audits;


import com.kingsrook.qqq.backend.core.model.actions.audits.AuditSingleInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.SupplementalCustomizerType;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 * Interface for classes that can be added to QInstance for customizing the
 * {@link AuditAction}.
 *******************************************************************************/
public interface AuditActionCustomizerInterface
{
   /***************************************************************************
    * Define the {@link SupplementalCustomizerType} needed when setting an
    * implementation of this interface into
    * {@link com.kingsrook.qqq.backend.core.model.metadata.QInstance#addSupplementalCustomizer(SupplementalCustomizerType, QCodeReference)}
    * <p>
    *    For example:
    * </p>
    * <code>qInstance.addSupplementalCustomizer(AuditActionCustomizerInterface.CUSTOMIZER_TYPE, new QCodeReference(MyAuditCustomizer.class));</code>
    ***************************************************************************/
   SupplementalCustomizerType CUSTOMIZER_TYPE = () -> AuditActionCustomizerInterface.class;


   /***************************************************************************
    * option to change values in an {@link AuditSingleInput} - e.g., before
    * an audit record is built.
    ***************************************************************************/
   default void customizeInput(AuditSingleInput auditSingleInput)
   {
      /////////////////////
      // noop by default //
      /////////////////////
   }

   /***************************************************************************
    * option to change values in a {@link QRecord}, after it has been populated
    * by the core audit action.
    *
    * <p>This might be a place where an application can add custom fields,
    * for example.</p>
    ***************************************************************************/
   default void customizeRecord(QRecord auditRecord, AuditSingleInput auditSingleInput)
   {
      /////////////////////
      // noop by default //
      /////////////////////
   }

}
