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

package com.kingsrook.sampleapp;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.templates.ConvertHtmlToPdfAction;
import com.kingsrook.qqq.backend.core.actions.templates.RenderTemplateAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.templates.ConvertHtmlToPdfInput;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXParseException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Sample PDF consumers inspect serialized bytes independently of the converter.
 *******************************************************************************/
@Timeout(30)
class SamplePdfAcceptanceTest
{
   @TempDir
   Path assets;

   private CapturedContext previousContext;



   /*******************************************************************************
    ** Use real sample metadata without starting a database, broker or HTTP server.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      previousContext = QContext.capture();
      QContext.init(SampleMetaDataProvider.defineTestInstance(), new QSession());
   }



   /*******************************************************************************
    ** Restore the caller context even after an assertion or conversion failure.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.init(previousContext);
   }



   /*******************************************************************************
    ** Literal page-specific text and an embedded image prove more than a PDF header.
    *******************************************************************************/
   @Test
   void testSampleTemplateProducesTwoReadablePagesAndLocalImage() throws Exception
   {
      writeLocalImage();
      assertNotNull(QContext.getQInstance().getTable("person"));
      String html = RenderTemplateAction.renderVelocity(
         Map.of("name", "Ada Sample", "table", QContext.getQInstance().getTable("person").getLabel()), resource("sample-report.html"));
      try(PDDocument pdf = PDDocument.load(convert(html)))
      {
         assertEquals(2, pdf.getNumberOfPages());
         assertEquals("Sample report\nAda Sample\nPerson\n", pageText(pdf, 1));
         assertEquals("Appendix\nReceipt PDF-571\n", pageText(pdf, 2));
         assertEquals(1, imageCount(pdf.getPage(0)));
         assertEquals(0, imageCount(pdf.getPage(1)));
      }
   }



   /*******************************************************************************
    ** Strict native XML rejects the fixture; QQQ's HTML repair retains its content.
    *******************************************************************************/
   @Test
   void testMalformedHtmlIsRepairedWithoutLosingText() throws Exception
   {
      String html = resource("malformed.html");
      Exception nativeFailure = assertThrows(Exception.class, () -> nativeConvert(html));
      assertInstanceOf(SAXParseException.class, rootCause(nativeFailure));
      try(PDDocument pdf = PDDocument.load(convert(html)))
      {
         assertEquals(1, pdf.getNumberOfPages());
         assertEquals("Repaired sample\nTea & cake\nTail marker\n", pageText(pdf, 1));
      }
   }



   /*******************************************************************************
    ** A missing image is tolerated by both entrypoints; an existing image embeds.
    *******************************************************************************/
   @Test
   void testMissingLocalAssetPreservesTextWithoutImage() throws Exception
   {
      String html = resource("local-asset.html");
      assertFalse(Files.exists(assets.resolve("owned.png")));
      for(byte[] bytes : List.of(nativeConvert(html), convert(html)))
      {
         try(PDDocument pdf = PDDocument.load(bytes))
         {
            assertEquals(1, pdf.getNumberOfPages());
            assertEquals("Local asset report\nBefore image\nAfter image\n", pageText(pdf, 1));
            assertEquals(0, imageCount(pdf.getPage(0)));
         }
      }
      writeLocalImage();
      for(byte[] bytes : List.of(nativeConvert(html), convert(html)))
      {
         try(PDDocument pdf = PDDocument.load(bytes))
         {
            assertEquals(1, pdf.getNumberOfPages());
            assertEquals("Local asset report\nBefore image\nAfter image\n", pageText(pdf, 1));
            assertEquals(1, imageCount(pdf.getPage(0)));
         }
      }
   }



   /*******************************************************************************
    ** The actual renderer hits the caller's failing sink; QQQ retains that cause.
    *******************************************************************************/
   @Test
   void testOutputFailureRetainsCauseAndNextConversionSucceeds() throws Exception
   {
      String html = "<html><body><p>Recovery PDF-571</p></body></html>";
      try(FailingOutputStream nativeOutput = new FailingOutputStream(); FailingOutputStream sampleOutput = new FailingOutputStream())
      {
         Exception nativeFailure = assertThrows(Exception.class, () -> new PdfRendererBuilder()
            .withHtmlContent(html, assets.toUri().toString()).toStream(nativeOutput).run());
         assertSame(nativeOutput.failure, rootCause(nativeFailure));
         assertEquals(64, nativeOutput.bytesWritten);

         QException failure = assertThrows(QException.class, () -> new ConvertHtmlToPdfAction().execute(
            new ConvertHtmlToPdfInput().withHtml(html).withBasePath(assets).withOutputStream(sampleOutput)));
         assertEquals("Error converting html to pdf", failure.getMessage());
         assertSame(sampleOutput.failure, rootCause(failure));
         assertEquals(64, sampleOutput.bytesWritten);
      }
      for(byte[] bytes : List.of(nativeConvert(html), convert(html)))
      {
         try(PDDocument pdf = PDDocument.load(bytes))
         {
            assertEquals(1, pdf.getNumberOfPages());
            assertEquals("Recovery PDF-571\n", pageText(pdf, 1));
         }
      }
   }



   /*******************************************************************************
    ** The native provider control isolates its behavior from QQQ's wrapper.
    *******************************************************************************/
   private byte[] nativeConvert(String html) throws Exception
   {
      try(ByteArrayOutputStream output = new ByteArrayOutputStream())
      {
         new PdfRendererBuilder().withHtmlContent(html, assets.toUri().toString()).toStream(output).run();
         return output.toByteArray();
      }
   }



   /*******************************************************************************
    ** Preserve the provider's causal evidence without depending on message wording.
    *******************************************************************************/
   private Throwable rootCause(Throwable failure)
   {
      while(failure.getCause() != null)
      {
         failure = failure.getCause();
      }
      return failure;
   }



   /*******************************************************************************
    ** All resource references resolve inside the per-test owned directory.
    *******************************************************************************/
   private byte[] convert(String html) throws Exception
   {
      try(ByteArrayOutputStream output = new ByteArrayOutputStream())
      {
         new ConvertHtmlToPdfAction().execute(new ConvertHtmlToPdfInput().withHtml(html).withBasePath(assets).withOutputStream(output));
         return output.toByteArray();
      }
   }



   /*******************************************************************************
    ** Load only first-party HTML, never external or private resources.
    *******************************************************************************/
   private String resource(String name) throws IOException
   {
      try(InputStream input = getClass().getResourceAsStream("/templates/pdf-acceptance/" + name))
      {
         assertNotNull(input, name);
         return new String(input.readAllBytes(), StandardCharsets.UTF_8);
      }
   }



   /*******************************************************************************
    ** PDFBox parses the finished output instead of trusting renderer success.
    *******************************************************************************/
   private String pageText(PDDocument pdf, int page) throws IOException
   {
      PDFTextStripper reader = new PDFTextStripper();
      reader.setStartPage(page);
      reader.setEndPage(page);
      reader.setLineSeparator("\n");
      return reader.getText(pdf);
   }



   /*******************************************************************************
    ** A native image object distinguishes asset loading from text-only fallback.
    *******************************************************************************/
   private int imageCount(PDPage page) throws IOException
   {
      int count = 0;
      for(var name : page.getResources().getXObjectNames())
      {
         if(page.getResources().getXObject(name) instanceof PDImageXObject)
         {
            count++;
         }
      }
      return count;
   }



   /*******************************************************************************
    ** A tiny generated image keeps the fixture local and independently observable.
    *******************************************************************************/
   private void writeLocalImage() throws IOException
   {
      BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
      image.setRGB(0, 0, 0x00AA44);
      assertTrue(ImageIO.write(image, "png", assets.resolve("owned.png").toFile()));
      assertTrue(Files.exists(assets.resolve("owned.png")));
   }


   /*******************************************************************************
    ** Fail only at the public output boundary after a small amount was written.
    *******************************************************************************/
   private static class FailingOutputStream extends OutputStream
   {
      private final IOException failure = new IOException("owned PDF sink failure");
      private int bytesWritten;



      /*******************************************************************************
       ** Partial output is intentional: a conversion error is not a rollback claim.
       *******************************************************************************/
      @Override
      public void write(int value) throws IOException
      {
         if(bytesWritten == 64)
         {
            throw failure;
         }
         bytesWritten++;
      }
   }

}
