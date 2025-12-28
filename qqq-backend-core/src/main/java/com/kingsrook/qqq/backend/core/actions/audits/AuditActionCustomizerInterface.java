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
