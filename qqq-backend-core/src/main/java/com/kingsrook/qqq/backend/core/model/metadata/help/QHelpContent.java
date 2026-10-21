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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;
import org.jsoup.Jsoup;


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
   public static final String SLOT_HELP = "help";

   private String summary;
   private String permissionName;
   private String clientChannel;
   private transient boolean fromData;
   private transient QHelpContent codeContent;

   private String        content;
   private HelpFormat    format;
   private Set<HelpRole> roles;

   ////////////////////////////////////
   // these appear to be thread safe //
   ////////////////////////////////////
   private static Parser       commonMarkParser   = Parser.builder().build();
   private static HtmlRenderer commonMarkRenderer = HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true).build();



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



   /*******************************************************************************
    ** Getter for summary
    *******************************************************************************/
   public String getSummary()
   {
      return (this.summary);
   }



   /*******************************************************************************
    ** Setter for summary
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    ** Fluent setter for summary
    *******************************************************************************/
   public QHelpContent withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for permissionName
    *******************************************************************************/
   @JsonIgnore
   public String getPermissionName()
   {
      return (this.permissionName);
   }



   /*******************************************************************************
    ** Setter for permissionName
    *******************************************************************************/
   public void setPermissionName(String permissionName)
   {
      this.permissionName = permissionName;
   }



   /*******************************************************************************
    ** Fluent setter for permissionName
    *******************************************************************************/
   public QHelpContent withPermissionName(String permissionName)
   {
      this.permissionName = permissionName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for clientChannel
    *******************************************************************************/
   @JsonIgnore
   public String getClientChannel()
   {
      return (this.clientChannel);
   }



   /*******************************************************************************
    ** Setter for clientChannel
    *******************************************************************************/
   public void setClientChannel(String clientChannel)
   {
      this.clientChannel = clientChannel;
   }



   /*******************************************************************************
    ** Fluent setter for clientChannel
    *******************************************************************************/
   public QHelpContent withClientChannel(String clientChannel)
   {
      this.clientChannel = clientChannel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fromData
    *******************************************************************************/
   @JsonIgnore
   public boolean getFromData()
   {
      return (this.fromData);
   }



   /*******************************************************************************
    ** Setter for fromData
    *******************************************************************************/
   public void setFromData(boolean fromData)
   {
      this.fromData = fromData;
   }



   /*******************************************************************************
    ** Fluent setter for fromData
    *******************************************************************************/
   public QHelpContent withFromData(boolean fromData)
   {
      this.fromData = fromData;
      return (this);
   }



   /*******************************************************************************
    ** Getter for codeContent
    *******************************************************************************/
   @JsonIgnore
   public QHelpContent getCodeContent()
   {
      return (this.codeContent);
   }



   /*******************************************************************************
    ** Setter for codeContent
    *******************************************************************************/
   public void setCodeContent(QHelpContent codeContent)
   {
      this.codeContent = codeContent;
   }



   /*******************************************************************************
    ** Fluent setter for codeContent
    *******************************************************************************/
   public QHelpContent withCodeContent(QHelpContent codeContent)
   {
      this.codeContent = codeContent;
      return (this);
   }



   /*******************************************************************************
    ** Plain text for search and derived summaries; never includes markup.
    *******************************************************************************/
   @JsonIgnore
   public String getPlainText()
   {
      if(!StringUtils.hasContent(content))
      {
         return ("");
      }
      String text = content;
      if(HelpFormat.MARKDOWN.equals(format))
      {
         text = TextContentRenderer.builder().build().render(commonMarkParser.parse(text));
      }
      if(!HelpFormat.TEXT.equals(format))
      {
         text = Jsoup.parse(text).text();
      }
      return (text.replaceAll("\\s+", " ").trim());
   }



   /*******************************************************************************
    ** Explicit summary, or the first plain-text sentence capped at 200 characters.
    *******************************************************************************/
   @JsonIgnore
   public String getSummaryOrDerived()
   {
      if(StringUtils.hasContent(summary))
      {
         return (summary);
      }
      String text = getPlainText();
      java.text.BreakIterator sentences = java.text.BreakIterator.getSentenceInstance(java.util.Locale.ROOT);
      sentences.setText(text);
      int end = sentences.next();
      if(end != java.text.BreakIterator.DONE)
      {
         text = text.substring(0, end).trim();
      }
      return (text.length() > 200 ? text.substring(0, 199) + "…" : text);
   }
}
