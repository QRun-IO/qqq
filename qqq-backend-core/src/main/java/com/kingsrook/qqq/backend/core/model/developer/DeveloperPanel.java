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

package com.kingsrook.qqq.backend.core.model.developer;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/*******************************************************************************
 ** One panel on a record's developer view, contributed by a
 ** {@link DeveloperPanelProviderInterface} (e.g., the ESB, quick search, or SQL
 ** workbench qbits).  A panel is a title plus an ordered list of generic
 ** {@link DeveloperPanelBlock}s, so a frontend can render any provider's panel
 ** without provider-specific code.
 *******************************************************************************/
public class DeveloperPanel implements Serializable
{
   /** Stable identifier of the panel (the provider's id), e.g., "esb". */
   private String id;

   /** Heading shown for the panel. */
   private String title;

   /** Sort order among panels (lower first). */
   private Integer order;

   /** Optional icon-name hint for the frontend (e.g., a Material icon name). */
   private String icon;

   /** Optional one-line description shown under the title. */
   private String description;

   /** Optional short status badge text, e.g., "Indexed". */
   private String statusLabel;

   /** Tone of the status badge. */
   private DeveloperPanelTone statusTone;

   /** The panel's content, in display order. */
   private List<DeveloperPanelBlock> blocks;

   /** Set (instead of blocks) when the provider failed to build its panel. */
   private String error;



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public String getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(String id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public DeveloperPanel withId(String id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for title
    *******************************************************************************/
   public String getTitle()
   {
      return (this.title);
   }



   /*******************************************************************************
    ** Setter for title
    *******************************************************************************/
   public void setTitle(String title)
   {
      this.title = title;
   }



   /*******************************************************************************
    ** Fluent setter for title
    *******************************************************************************/
   public DeveloperPanel withTitle(String title)
   {
      this.title = title;
      return (this);
   }



   /*******************************************************************************
    ** Getter for order
    *******************************************************************************/
   public Integer getOrder()
   {
      return (this.order);
   }



   /*******************************************************************************
    ** Setter for order
    *******************************************************************************/
   public void setOrder(Integer order)
   {
      this.order = order;
   }



   /*******************************************************************************
    ** Fluent setter for order
    *******************************************************************************/
   public DeveloperPanel withOrder(Integer order)
   {
      this.order = order;
      return (this);
   }



   /*******************************************************************************
    ** Getter for icon
    *******************************************************************************/
   public String getIcon()
   {
      return (this.icon);
   }



   /*******************************************************************************
    ** Setter for icon
    *******************************************************************************/
   public void setIcon(String icon)
   {
      this.icon = icon;
   }



   /*******************************************************************************
    ** Fluent setter for icon
    *******************************************************************************/
   public DeveloperPanel withIcon(String icon)
   {
      this.icon = icon;
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
   public DeveloperPanel withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for statusLabel
    *******************************************************************************/
   public String getStatusLabel()
   {
      return (this.statusLabel);
   }



   /*******************************************************************************
    ** Setter for statusLabel
    *******************************************************************************/
   public void setStatusLabel(String statusLabel)
   {
      this.statusLabel = statusLabel;
   }



   /*******************************************************************************
    ** Fluent setter for statusLabel
    *******************************************************************************/
   public DeveloperPanel withStatusLabel(String statusLabel)
   {
      this.statusLabel = statusLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for statusTone
    *******************************************************************************/
   public DeveloperPanelTone getStatusTone()
   {
      return (this.statusTone);
   }



   /*******************************************************************************
    ** Setter for statusTone
    *******************************************************************************/
   public void setStatusTone(DeveloperPanelTone statusTone)
   {
      this.statusTone = statusTone;
   }



   /*******************************************************************************
    ** Fluent setter for statusTone
    *******************************************************************************/
   public DeveloperPanel withStatusTone(DeveloperPanelTone statusTone)
   {
      this.statusTone = statusTone;
      return (this);
   }



   /*******************************************************************************
    ** Getter for blocks
    *******************************************************************************/
   public List<DeveloperPanelBlock> getBlocks()
   {
      return (this.blocks);
   }



   /*******************************************************************************
    ** Setter for blocks
    *******************************************************************************/
   public void setBlocks(List<DeveloperPanelBlock> blocks)
   {
      this.blocks = blocks;
   }



   /*******************************************************************************
    ** Fluent setter for blocks
    *******************************************************************************/
   public DeveloperPanel withBlocks(List<DeveloperPanelBlock> blocks)
   {
      this.blocks = blocks;
      return (this);
   }



   /*******************************************************************************
    ** Fluent adder for blocks - adds one block.
    *******************************************************************************/
   public DeveloperPanel withBlock(DeveloperPanelBlock block)
   {
      if(this.blocks == null)
      {
         this.blocks = new ArrayList<>();
      }
      this.blocks.add(block);
      return (this);
   }



   /*******************************************************************************
    ** Getter for error
    *******************************************************************************/
   public String getError()
   {
      return (this.error);
   }



   /*******************************************************************************
    ** Setter for error
    *******************************************************************************/
   public void setError(String error)
   {
      this.error = error;
   }



   /*******************************************************************************
    ** Fluent setter for error
    *******************************************************************************/
   public DeveloperPanel withError(String error)
   {
      this.error = error;
      return (this);
   }

}
