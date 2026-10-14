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

package com.kingsrook.qqq.backend.core.model.actions.consistentread;


import java.util.Optional;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;


/*******************************************************************************
 ** Input for QBackendModuleInterface.openConsistentReadTransaction - asks a
 ** backend for a read-only transaction through which every table read reflects
 ** one instant (see ConsistencyScope).
 *******************************************************************************/
public class ConsistentReadInput extends AbstractActionInput
{
   private String           backendName;
   private ConsistencyScope scope            = ConsistencyScope.BACKEND;
   private boolean          readOnly         = true;
   private boolean          useReadReplica   = false;
   private String           importSnapshotId = null;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public ConsistentReadInput()
   {
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public ConsistentReadInput(String backendName)
   {
      this.backendName = backendName;
   }



   /*******************************************************************************
    ** Getter for backendName
    *******************************************************************************/
   public String getBackendName()
   {
      return (this.backendName);
   }



   /*******************************************************************************
    ** Setter for backendName
    *******************************************************************************/
   public void setBackendName(String backendName)
   {
      this.backendName = backendName;
   }



   /*******************************************************************************
    ** Fluent setter for backendName
    *******************************************************************************/
   public ConsistentReadInput withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scope
    *******************************************************************************/
   public ConsistencyScope getScope()
   {
      return (this.scope);
   }



   /*******************************************************************************
    ** Setter for scope
    *******************************************************************************/
   public void setScope(ConsistencyScope scope)
   {
      this.scope = scope;
   }



   /*******************************************************************************
    ** Fluent setter for scope
    *******************************************************************************/
   public ConsistentReadInput withScope(ConsistencyScope scope)
   {
      this.scope = scope;
      return (this);
   }



   /*******************************************************************************
    ** Getter for readOnly
    *******************************************************************************/
   public boolean getReadOnly()
   {
      return (this.readOnly);
   }



   /*******************************************************************************
    ** Setter for readOnly
    *******************************************************************************/
   public void setReadOnly(boolean readOnly)
   {
      this.readOnly = readOnly;
   }



   /*******************************************************************************
    ** Fluent setter for readOnly
    *******************************************************************************/
   public ConsistentReadInput withReadOnly(boolean readOnly)
   {
      this.readOnly = readOnly;
      return (this);
   }



   /*******************************************************************************
    ** Getter for useReadReplica
    *******************************************************************************/
   public boolean getUseReadReplica()
   {
      return (this.useReadReplica);
   }



   /*******************************************************************************
    ** Setter for useReadReplica
    *******************************************************************************/
   public void setUseReadReplica(boolean useReadReplica)
   {
      this.useReadReplica = useReadReplica;
   }



   /*******************************************************************************
    ** Fluent setter for useReadReplica
    *******************************************************************************/
   public ConsistentReadInput withUseReadReplica(boolean useReadReplica)
   {
      this.useReadReplica = useReadReplica;
      return (this);
   }



   /*******************************************************************************
    ** Getter for importSnapshotId - if present, attach to the snapshot exported
    ** by another consistent-read transaction (getExportedSnapshotId) instead of
    ** taking a new one.
    *******************************************************************************/
   public Optional<String> getImportSnapshotId()
   {
      return (Optional.ofNullable(this.importSnapshotId));
   }



   /*******************************************************************************
    ** Setter for importSnapshotId
    *******************************************************************************/
   public void setImportSnapshotId(String importSnapshotId)
   {
      this.importSnapshotId = importSnapshotId;
   }



   /*******************************************************************************
    ** Fluent setter for importSnapshotId
    *******************************************************************************/
   public ConsistentReadInput withImportSnapshotId(String importSnapshotId)
   {
      this.importSnapshotId = importSnapshotId;
      return (this);
   }

}
