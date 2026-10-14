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
 ** One generic block of content in a {@link DeveloperPanel}.  The {@link #type}
 ** says which of the other fields are used:
 ** <ul>
 ** <li>KEY_VALUES - {@link #items}</li>
 ** <li>TEXT - {@link #text} (plain text), with an optional {@link #tone}</li>
 ** <li>CODE - {@link #code} in a {@link #language} (e.g., json, sql, shell)</li>
 ** <li>TABLE - {@link #columns} and {@link #rows} (all values as display strings)</li>
 ** <li>LINKS - {@link #links}</li>
 ** <li>ACTIONS - {@link #actions} (processes the frontend may launch for the record)</li>
 ** </ul>
 ** Every block may have a {@link #label} shown as a sub-heading.
 *******************************************************************************/
public class DeveloperPanelBlock implements Serializable
{
   /** Which kind of block this is. */
   private DeveloperPanelBlockType type;

   /** Optional sub-heading. */
   private String label;

   /** Plain text, for TEXT blocks. */
   private String text;

   /** Optional tone for TEXT blocks (info, success, warning, error). */
   private DeveloperPanelTone tone;

   /** Language of a CODE block (json, sql, shell, javascript, java ...). */
   private String language;

   /** The source of a CODE block. */
   private String code;

   /** Label/value pairs, for KEY_VALUES blocks. */
   private List<DeveloperPanelKeyValue> items;

   /** Column headings, for TABLE blocks. */
   private List<String> columns;

   /** Rows of display values, for TABLE blocks. */
   private List<List<String>> rows;

   /** Links, for LINKS blocks. */
   private List<DeveloperPanelLink> links;

   /** Process actions, for ACTIONS blocks. */
   private List<DeveloperPanelAction> actions;


   /*******************************************************************************
    ** Make a KEY_VALUES block.
    *******************************************************************************/
   public static DeveloperPanelBlock keyValues(String label)
   {
      return (new DeveloperPanelBlock().withType(DeveloperPanelBlockType.KEY_VALUES).withLabel(label).withItems(new ArrayList<>()));
   }



   /*******************************************************************************
    ** Make a TEXT block.
    *******************************************************************************/
   public static DeveloperPanelBlock text(String text, DeveloperPanelTone tone)
   {
      return (new DeveloperPanelBlock().withType(DeveloperPanelBlockType.TEXT).withText(text).withTone(tone));
   }



   /*******************************************************************************
    ** Make a CODE block.
    *******************************************************************************/
   public static DeveloperPanelBlock code(String label, String language, String code)
   {
      return (new DeveloperPanelBlock().withType(DeveloperPanelBlockType.CODE).withLabel(label).withLanguage(language).withCode(code));
   }



   /*******************************************************************************
    ** Make a TABLE block with the given column headings (add rows with withRow).
    *******************************************************************************/
   public static DeveloperPanelBlock table(String label, List<String> columns)
   {
      return (new DeveloperPanelBlock().withType(DeveloperPanelBlockType.TABLE).withLabel(label).withColumns(new ArrayList<>(columns)).withRows(new ArrayList<>()));
   }



   /*******************************************************************************
    ** Make a LINKS block.
    *******************************************************************************/
   public static DeveloperPanelBlock links(String label)
   {
      return (new DeveloperPanelBlock().withType(DeveloperPanelBlockType.LINKS).withLabel(label).withLinks(new ArrayList<>()));
   }



   /*******************************************************************************
    ** Make an ACTIONS block.
    *******************************************************************************/
   public static DeveloperPanelBlock actions(String label)
   {
      return (new DeveloperPanelBlock().withType(DeveloperPanelBlockType.ACTIONS).withLabel(label).withActions(new ArrayList<>()));
   }



   /*******************************************************************************
    ** Fluent adder for a KEY_VALUES item.
    *******************************************************************************/
   public DeveloperPanelBlock withItem(String label, Object value)
   {
      return (withItem(new DeveloperPanelKeyValue().withLabel(label).withValue(value == null ? null : String.valueOf(value))));
   }



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public DeveloperPanelBlockType getType()
   {
      return (this.type);
   }



   /*******************************************************************************
    ** Setter for type
    *******************************************************************************/
   public void setType(DeveloperPanelBlockType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    *******************************************************************************/
   public DeveloperPanelBlock withType(DeveloperPanelBlockType type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Setter for label
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public DeveloperPanelBlock withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for text
    *******************************************************************************/
   public String getText()
   {
      return (this.text);
   }



   /*******************************************************************************
    ** Setter for text
    *******************************************************************************/
   public void setText(String text)
   {
      this.text = text;
   }



   /*******************************************************************************
    ** Fluent setter for text
    *******************************************************************************/
   public DeveloperPanelBlock withText(String text)
   {
      this.text = text;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tone
    *******************************************************************************/
   public DeveloperPanelTone getTone()
   {
      return (this.tone);
   }



   /*******************************************************************************
    ** Setter for tone
    *******************************************************************************/
   public void setTone(DeveloperPanelTone tone)
   {
      this.tone = tone;
   }



   /*******************************************************************************
    ** Fluent setter for tone
    *******************************************************************************/
   public DeveloperPanelBlock withTone(DeveloperPanelTone tone)
   {
      this.tone = tone;
      return (this);
   }



   /*******************************************************************************
    ** Getter for language
    *******************************************************************************/
   public String getLanguage()
   {
      return (this.language);
   }



   /*******************************************************************************
    ** Setter for language
    *******************************************************************************/
   public void setLanguage(String language)
   {
      this.language = language;
   }



   /*******************************************************************************
    ** Fluent setter for language
    *******************************************************************************/
   public DeveloperPanelBlock withLanguage(String language)
   {
      this.language = language;
      return (this);
   }



   /*******************************************************************************
    ** Getter for code
    *******************************************************************************/
   public String getCode()
   {
      return (this.code);
   }



   /*******************************************************************************
    ** Setter for code
    *******************************************************************************/
   public void setCode(String code)
   {
      this.code = code;
   }



   /*******************************************************************************
    ** Fluent setter for code
    *******************************************************************************/
   public DeveloperPanelBlock withCode(String code)
   {
      this.code = code;
      return (this);
   }



   /*******************************************************************************
    ** Getter for items
    *******************************************************************************/
   public List<DeveloperPanelKeyValue> getItems()
   {
      return (this.items);
   }



   /*******************************************************************************
    ** Setter for items
    *******************************************************************************/
   public void setItems(List<DeveloperPanelKeyValue> items)
   {
      this.items = items;
   }



   /*******************************************************************************
    ** Fluent setter for items
    *******************************************************************************/
   public DeveloperPanelBlock withItems(List<DeveloperPanelKeyValue> items)
   {
      this.items = items;
      return (this);
   }



   /*******************************************************************************
    ** Fluent adder for items - adds one item.
    *******************************************************************************/
   public DeveloperPanelBlock withItem(DeveloperPanelKeyValue item)
   {
      if(this.items == null)
      {
         this.items = new ArrayList<>();
      }
      this.items.add(item);
      return (this);
   }



   /*******************************************************************************
    ** Getter for columns
    *******************************************************************************/
   public List<String> getColumns()
   {
      return (this.columns);
   }



   /*******************************************************************************
    ** Setter for columns
    *******************************************************************************/
   public void setColumns(List<String> columns)
   {
      this.columns = columns;
   }



   /*******************************************************************************
    ** Fluent setter for columns
    *******************************************************************************/
   public DeveloperPanelBlock withColumns(List<String> columns)
   {
      this.columns = columns;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rows
    *******************************************************************************/
   public List<List<String>> getRows()
   {
      return (this.rows);
   }



   /*******************************************************************************
    ** Setter for rows
    *******************************************************************************/
   public void setRows(List<List<String>> rows)
   {
      this.rows = rows;
   }



   /*******************************************************************************
    ** Fluent setter for rows
    *******************************************************************************/
   public DeveloperPanelBlock withRows(List<List<String>> rows)
   {
      this.rows = rows;
      return (this);
   }



   /*******************************************************************************
    ** Fluent adder for rows - adds one row.
    *******************************************************************************/
   public DeveloperPanelBlock withRow(List<String> row)
   {
      if(this.rows == null)
      {
         this.rows = new ArrayList<>();
      }
      this.rows.add(row);
      return (this);
   }



   /*******************************************************************************
    ** Getter for links
    *******************************************************************************/
   public List<DeveloperPanelLink> getLinks()
   {
      return (this.links);
   }



   /*******************************************************************************
    ** Setter for links
    *******************************************************************************/
   public void setLinks(List<DeveloperPanelLink> links)
   {
      this.links = links;
   }



   /*******************************************************************************
    ** Fluent setter for links
    *******************************************************************************/
   public DeveloperPanelBlock withLinks(List<DeveloperPanelLink> links)
   {
      this.links = links;
      return (this);
   }



   /*******************************************************************************
    ** Fluent adder for links - adds one link.
    *******************************************************************************/
   public DeveloperPanelBlock withLink(DeveloperPanelLink link)
   {
      if(this.links == null)
      {
         this.links = new ArrayList<>();
      }
      this.links.add(link);
      return (this);
   }



   /*******************************************************************************
    ** Getter for actions
    *******************************************************************************/
   public List<DeveloperPanelAction> getActions()
   {
      return (this.actions);
   }



   /*******************************************************************************
    ** Setter for actions
    *******************************************************************************/
   public void setActions(List<DeveloperPanelAction> actions)
   {
      this.actions = actions;
   }



   /*******************************************************************************
    ** Fluent setter for actions
    *******************************************************************************/
   public DeveloperPanelBlock withActions(List<DeveloperPanelAction> actions)
   {
      this.actions = actions;
      return (this);
   }



   /*******************************************************************************
    ** Fluent adder for actions - adds one action.
    *******************************************************************************/
   public DeveloperPanelBlock withAction(DeveloperPanelAction action)
   {
      if(this.actions == null)
      {
         this.actions = new ArrayList<>();
      }
      this.actions.add(action);
      return (this);
   }

}
