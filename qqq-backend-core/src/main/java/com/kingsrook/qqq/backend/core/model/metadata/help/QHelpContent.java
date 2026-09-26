/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.help;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;


/*******************************************************************************
 ** meta-data definition of "Help Content" to show to a user - for use in
 ** a specific "role" (e.g., insert screens but not view screens), and in a
 ** particular "format" (e.g., plain text, html, markdown).
 **
 ** Meant to be assigned to several different pieces of QQQ meta data (fields,
 ** tables, processes, etc), and used as-needed by various frontends.
 **
 ** May evolve something like a "Presentation" attribute in the future - e.g.,
 ** to say "present this one as a tooltip" vs. "present this one as inline text"
 **
 ** May be dynamically added to meta-data via (non-meta-) data - see
 ** HelpContentMetaDataProvider and QInstanceHelpContentManager
 *******************************************************************************/
public class QHelpContent implements QMetaDataObject, Cloneable
{
   private String        content;
   private HelpFormat    format;
   private Set<HelpRole> roles;

   ////////////////////////////////////
   // these appear to be thread safe //
   ////////////////////////////////////
   private static Parser       commonMarkParser   = Parser.builder().build();
   private static HtmlRenderer commonMarkRenderer = HtmlRenderer.builder().build();



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QHelpContent()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QHelpContent(String content)
   {
      setContent(content);
   }



   /***************************************************************************
    * Return the content as html string, based on its format.
    * Only MARKDOWN actually gets processed (via commonmark) - but TEXT and
    * HTML come out as-is.
    ***************************************************************************/
   public String getContentAsHtml()
   {
      if(content == null)
      {
         return (null);
      }

      if(HelpFormat.MARKDOWN.equals(this.format))
      {
         //////////////////////////////
         // convert markdown to HTML //
         //////////////////////////////
         Node   document = commonMarkParser.parse(content);
         String html     = commonMarkRenderer.render(document);
         return (html);
      }
      else
      {
         ///////////////////////////////////////////////////
         // other formats (html & text) just output as-is //
         ///////////////////////////////////////////////////
         return (content);
      }
   }



   /*******************************************************************************
    ** Getter for content
    *******************************************************************************/
   public String getContent()
   {
      return (this.content);
   }



   /*******************************************************************************
    ** Setter for content
    *******************************************************************************/
   public void setContent(String content)
   {
      this.content = content;
   }



   /*******************************************************************************
    ** Fluent setter for content
    *******************************************************************************/
   public QHelpContent withContent(String content)
   {
      this.content = content;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for content that also sets format as HTML
    *******************************************************************************/
   public QHelpContent withContentAsHTML(String content)
   {
      this.content = content;
      this.format = HelpFormat.HTML;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for content that also sets format as TEXT
    *******************************************************************************/
   public QHelpContent withContentAsText(String content)
   {
      this.content = content;
      this.format = HelpFormat.TEXT;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for content that also sets format as Markdown
    *******************************************************************************/
   public QHelpContent withContentAsMarkdown(String content)
   {
      this.content = content;
      this.format = HelpFormat.MARKDOWN;
      return (this);
   }



   /*******************************************************************************
    ** Getter for format
    *******************************************************************************/
   public HelpFormat getFormat()
   {
      return (this.format);
   }



   /*******************************************************************************
    ** Setter for format
    *******************************************************************************/
   public void setFormat(HelpFormat format)
   {
      this.format = format;
   }



   /*******************************************************************************
    ** Fluent setter for format
    *******************************************************************************/
   public QHelpContent withFormat(HelpFormat format)
   {
      this.format = format;
      return (this);
   }



   /*******************************************************************************
    ** Getter for roles
    *******************************************************************************/
   public Set<HelpRole> getRoles()
   {
      return (this.roles);
   }



   /*******************************************************************************
    ** Setter for roles
    *******************************************************************************/
   public void setRoles(Set<HelpRole> roles)
   {
      this.roles = roles;
   }



   /*******************************************************************************
    ** Fluent setter for roles
    *******************************************************************************/
   public QHelpContent withRoles(Set<HelpRole> roles)
   {
      this.roles = roles;
      return (this);
   }



   /*******************************************************************************
    ** Fluent method to add a role
    *******************************************************************************/
   public QHelpContent withRole(HelpRole role)
   {
      return (withRoles(role));
   }



   /*******************************************************************************
    ** Fluent method to add a role
    *******************************************************************************/
   public QHelpContent withRoles(HelpRole... roles)
   {
      if(roles == null || roles.length == 0)
      {
         return (this);
      }

      if(this.roles == null)
      {
         this.roles = new HashSet<>();
      }

      Collections.addAll(this.roles, roles);

      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public QHelpContent clone()
   {
      try
      {
         QHelpContent clone = (QHelpContent) super.clone();

         if(roles != null)
         {
            clone.roles = new HashSet<>(roles);
         }

         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }
}
