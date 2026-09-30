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

package com.kingsrook.qqq.backend.core.actions.processes;


import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.google.common.hash.Hashing;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Ownership of the current JVM's process state, separate from client values.
 ** A serialized state cannot grant access in another application instance.
 *******************************************************************************/
public final class ProcessStateAccess implements Serializable
{
   private final transient QInstance instance;
   private final String sessionHash;
   private final Map<String, Serializable> backendVariants;
   private final String processName;
   private final String processUUID;



   /*******************************************************************************
    **
    *******************************************************************************/
   private ProcessStateAccess(String processName, String processUUID) throws QAuthenticationException
   {
      QSession session = QContext.getQSession();
      if(QContext.getQInstance() == null || session == null || session.getUuid() == null)
      {
         throw new QAuthenticationException("A session is required for process state");
      }
      this.instance = QContext.getQInstance();
      this.sessionHash = sessionHash(session);
      this.backendVariants = session.getBackendVariants() == null ? Map.of() : new HashMap<>(session.getBackendVariants());
      this.processName = processName;
      this.processUUID = processUUID;
   }



   /*******************************************************************************
    ** Capture before an asynchronous job can run or expose a job identifier.
    *******************************************************************************/
   public static ProcessStateAccess capture(String processName, String processUUID) throws QAuthenticationException
   {
      return new ProcessStateAccess(processName, processUUID);
   }



   /*******************************************************************************
    ** Unowned internal state cannot be made public by presenting its identifier.
    *******************************************************************************/
   public static void requireAccess(ProcessStateAccess access, String expectedName, String expectedUUID) throws QException
   {
      if(access == null)
      {
         throw new QPermissionDeniedException("Permission denied for process state.");
      }
      access.requireAccess(expectedName, expectedUUID);
   }



   /*******************************************************************************
    ** A null expected name is used by process-dependent possible-value lookups.
    *******************************************************************************/
   public void requireAccess(String expectedName, String expectedUUID) throws QException
   {
      QSession session = QContext.getQSession();
      if(instance == null || instance != QContext.getQInstance() || session == null
         || !sessionHash.equals(sessionHash(session))
         || !backendVariants.equals(session.getBackendVariants() == null ? Map.of() : session.getBackendVariants())
         || (expectedName != null && !Objects.equals(processName, expectedName))
         || !Objects.equals(processUUID, expectedUUID))
      {
         throw new QPermissionDeniedException("Permission denied for process state.");
      }
      PermissionsHelper.checkProcessPermissionThrowing(new AbstractActionInput(), processName);
   }



   /*******************************************************************************
    ** Store only a digest of authentication references, which may be credentials.
    *******************************************************************************/
   private static String sessionHash(QSession session)
   {
      String reference = StringUtils.hasContent(session.getIdReference()) ? "reference:" + session.getIdReference() : "uuid:" + session.getUuid();
      return Hashing.sha256().hashString(reference, StandardCharsets.UTF_8).toString();
   }
}
