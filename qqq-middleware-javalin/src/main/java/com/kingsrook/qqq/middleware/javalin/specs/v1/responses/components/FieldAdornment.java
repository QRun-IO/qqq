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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.io.Serializable;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIEnumSubSet;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 **
 *******************************************************************************/
public class FieldAdornment implements ToSchema
{
   @OpenAPIExclude()
   private com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FieldAdornment(com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FieldAdornment()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class FieldAdornmentSubSet implements OpenAPIEnumSubSet.EnumSubSet<AdornmentType>
   {
      private static EnumSet<AdornmentType> subSet = null;



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public EnumSet<AdornmentType> getSubSet()
      {
         if(subSet == null)
         {
            EnumSet<AdornmentType> subSet = EnumSet.allOf(AdornmentType.class);
            FieldAdornmentSubSet.subSet = subSet;
         }

         return (subSet);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Type of this adornment")
   @OpenAPIEnumSubSet(FieldAdornmentSubSet.class)
   public AdornmentType getType()
   {
      return (this.wrapped == null || this.wrapped.getType() == null ? null : this.wrapped.getType());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Values associated with this adornment.  Keys and the meanings of their values will differ by type.")
   public Map<String, Serializable> getValues()
   {
      return (this.wrapped.getValues());
   }


   /***************************************************************************
    ** The generated schema, with the known keys of `values` described (they
    ** differ by adornment type; unknown keys may appear and are ignored).
    ***************************************************************************/
   @Override
   public Schema toSchema()
   {
      Schema schema = ToSchema.super.toSchema();
      Schema values = schema.getProperties() == null ? null : schema.getProperties().get("values");
      if(values != null)
      {
         values.withProperties(knownValueSchemas());
         values.withDescription("""
            Values associated with this adornment.  Keys and the meanings of their values differ by type; the known
            keys are listed (each names the type(s) it applies to).  CHIP also uses pattern keys: `color.{fieldValue}`
            (one of default, info, primary, secondary, success, warning, error) and `icon.{fieldValue}` (an icon name),
            giving the chip colour and icon for a field value.  Clients ignore keys they do not know.""");
      }
      return (schema);
   }



   /***************************************************************************
    ** known keys of adornment values, by name
    ***************************************************************************/
   static Map<String, Schema> knownValueSchemas()
   {
      Map<String, Schema> rs = new LinkedHashMap<>();
      rs.put("target", str("LINK: where to open the link: _blank for a new window/tab; otherwise the same view."));
      rs.put("toRecordFromTable", str("LINK: the field value is a primary key of this table; link to that record (a client navigates to the record view)."));
      rs.put("toRecordFromTableDynamic", bool("LINK: the target table is per record - read it from the record displayValue `{fieldName}:toRecordFromTableDynamic`."));
      rs.put("fileNameField", str("FILE_DOWNLOAD, FILE_PREVIEW: the field holding the file name to save or show."));
      rs.put("defaultExtension", str("FILE_DOWNLOAD: extension for the saved file when the name has none."));
      rs.put("defaultMimeType", str("FILE_DOWNLOAD, FILE_PREVIEW: MIME type to assume when none is known."));
      rs.put("supplementalProcessName", str("FILE_DOWNLOAD: process the server runs to produce the file (the client just downloads)."));
      rs.put("supplementalCodeReference", new Schema().withType(Type.OBJECT).withDescription("FILE_DOWNLOAD: code the server runs to produce the file (the client just downloads).").withAdditionalProperties(true));
      rs.put("downloadUrlDynamic", bool("FILE_DOWNLOAD: the download URL is per record - read it from the record displayValue `{fieldName}:downloadUrlDynamic`."));
      rs.put("fileNameFormat", str("FILE_DOWNLOAD: format (with %s placeholders) for the saved file name."));
      rs.put("fileNameFormatFields", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("FILE_DOWNLOAD: fields whose values fill fileNameFormat, in order."));
      rs.put("width", str("SIZE: display width of the field - one of xsmall, small, medium, medlarge, large, xlarge (semantic widths; each client maps them to its own columns or characters). FILE_UPLOAD: width of the upload control.")
         .withEnumValues(List.of("xsmall", "small", "medium", "medlarge", "large", "xlarge")));
      rs.put("format", str("FILE_UPLOAD: accepted file formats (e.g. .csv,.xlsx)."));
      rs.put("languageMode", str("CODE_EDITOR: language of the code (e.g. javascript, sql, json, velocity) - for highlighting; a client without an editor shows it monospaced."));
      rs.put("staticText", str("TOOLTIP: the tooltip text."));
      rs.put("tooltipDynamic", bool("TOOLTIP: the text is per record - read it from the record displayValue `{fieldName}:tooltipDynamic`."));
      rs.put("thumbnailUrlField", str("FILE_PREVIEW: field holding a thumbnail image URL."));
      rs.put("fullSizeUrlField", str("FILE_PREVIEW: field holding the full-size file URL."));
      rs.put("mimeTypeField", str("FILE_PREVIEW: field holding the MIME type (decides image, video, PDF or download-only)."));
      rs.put("altTextField", str("FILE_PREVIEW: field holding alternative text."));
      rs.put("captionField", str("FILE_PREVIEW: field holding a caption."));
      rs.put("widthField", str("FILE_PREVIEW: field holding the pixel width."));
      rs.put("heightField", str("FILE_PREVIEW: field holding the pixel height."));
      rs.put("sizeBytesField", str("FILE_PREVIEW: field holding the size in bytes."));
      rs.put("thumbnailMaxSize", new Schema().withType(Type.INTEGER).withDescription("FILE_PREVIEW: largest thumbnail edge in pixels."));
      rs.put("previewable", bool("FILE_PREVIEW: whether the file may be previewed (false: download only)."));
      rs.put("widgetName", str("WIDGET: the widget that renders this field."));
      return (rs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static Schema str(String description)
   {
      return (new Schema().withType(Type.STRING).withDescription(description));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static Schema bool(String description)
   {
      return (new Schema().withType(Type.BOOLEAN).withDescription(description));
   }
}
