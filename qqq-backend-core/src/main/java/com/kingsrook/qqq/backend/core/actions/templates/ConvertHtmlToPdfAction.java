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

package com.kingsrook.qqq.backend.core.actions.templates;


import java.nio.file.Path;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.AbstractQActionFunction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.templates.ConvertHtmlToPdfInput;
import com.kingsrook.qqq.backend.core.model.actions.templates.ConvertHtmlToPdfOutput;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver;
import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;


/*******************************************************************************
 ** Action to convert a string of HTML to a PDF!
 **
 ** Much credit to https://www.baeldung.com/java-html-to-pdf
 **
 ** Updated in March 2025 to go from flying-saucer-pdf-openpdf lib to openhtmltopdf,
 ** mostly to get support for max-height on images...
 ********************************************************************************/
public class ConvertHtmlToPdfAction extends AbstractQActionFunction<ConvertHtmlToPdfInput, ConvertHtmlToPdfOutput>
{
   private static final QLogger LOG = QLogger.getLogger(ConvertHtmlToPdfAction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ConvertHtmlToPdfOutput execute(ConvertHtmlToPdfInput input) throws QException
   {
      try
      {
         ConvertHtmlToPdfOutput output = new ConvertHtmlToPdfOutput();

         //////////////////////////////////////////////////////////////////
         // convert the input HTML to XHTML, as needed for ITextRenderer //
         //////////////////////////////////////////////////////////////////
         Document document = Jsoup.parse(input.getHtml());
         document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
         org.w3c.dom.Document w3cDoc = new W3CDom().fromJsoup(document);

         //////////////////////////////
         // convert the XHTML to PDF //
         //////////////////////////////
         PdfRendererBuilder builder = new PdfRendererBuilder();
         builder.toStream(input.getOutputStream());
         builder.useFastMode();
         builder.withW3cDocument(w3cDoc, input.getBasePath() == null ? "./" : input.getBasePath().toUri().toString());

         try(PdfBoxRenderer pdfBoxRenderer = builder.buildPdfRenderer())
         {
            pdfBoxRenderer.layout();
            pdfBoxRenderer.getSharedContext().setPrint(true);
            pdfBoxRenderer.getSharedContext().setInteractive(false);

            for(Map.Entry<String, Path> entry : CollectionUtils.nonNullMap(input.getCustomFonts()).entrySet())
            {
               LOG.warn("Note:  Custom fonts appear to not be working in this class at this time...");
               pdfBoxRenderer.getFontResolver().addFont(
                  entry.getValue().toAbsolutePath().toFile(), // Path to the TrueType font file
                  entry.getKey(),                             // Font family name to use in CSS
                  400,                                        // Font weight (e.g., 400 for normal, 700 for bold)
                  IdentValue.NORMAL,                          // Font style (e.g., NORMAL, ITALIC)
                  true,                                       // Whether to subset the font
                  PdfBoxFontResolver.FontGroup.MAIN           // ??
               );
            }

            pdfBoxRenderer.createPDF();
         }

         return (output);
      }
      catch(Exception e)
      {
         throw (new QException("Error converting html to pdf", e));
      }
   }

}
