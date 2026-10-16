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

package com.kingsrook.qqq.backend.core.actions.profile;


import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.metadata.profile.UserProfilesMetaData;


/*******************************************************************************
 ** Turns an uploaded image into the standard avatar sizes, safely:
 **
 ** - the type comes from the file's magic bytes only (PNG, JPEG, GIF, WebP);
 **   SVG, HTML and everything else is refused, so no script can be served;
 ** - the size and the pixel dimensions are bounded before decoding (a
 **   decompression bomb is refused from its header);
 ** - the image is decoded, centre-cropped square, turned upright (JPEG EXIF
 **   orientation), resized, and re-encoded as PNG: re-encoding drops all
 **   metadata (EXIF and its GPS position, XMP, comments).
 **
 ** Inline and bounded (a few hundred milliseconds at the limits), so it runs
 ** in the request rather than as a durable job; at most MAX_CONCURRENT_DECODES
 ** run at once per node. Everything stays in memory: ImageIO's file cache is
 ** never used (no temporary files).
 *******************************************************************************/
public final class AvatarImageProcessor
{
   static final String REFUSED_TYPE = "Use a PNG, JPEG, WebP or GIF image.";

   private static final int MAX_EDGE   = 20_000;
   private static final int WORK_EDGE  = 256;
   private static final int DECODE_CAP = 1024;

   private static final Semaphore DECODES = new Semaphore(UserProfilesMetaData.MAX_CONCURRENT_DECODES, true);



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private AvatarImageProcessor()
   {
   }



   /*******************************************************************************
    ** Read at most maxBytes from the stream; refuse anything larger.
    *******************************************************************************/
   public static byte[] readBounded(InputStream inputStream, long maxBytes) throws IOException, QBadRequestException
   {
      ByteArrayOutputStream out    = new ByteArrayOutputStream();
      byte[]                buffer = new byte[64 * 1024];
      long                  total  = 0;
      int                   read;
      while((read = inputStream.read(buffer)) != -1)
      {
         total += read;
         if(total > maxBytes)
         {
            throw (new QBadRequestException("That image is too large: the limit is " + (maxBytes / (1024 * 1024)) + " MB."));
         }
         out.write(buffer, 0, read);
      }
      return (out.toByteArray());
   }



   /*******************************************************************************
    ** The image format named by the file's first bytes, or null when it is not an
    ** accepted raster type.  Never trusts a file name or a declared content type.
    *******************************************************************************/
   public static String detectFormat(byte[] bytes)
   {
      if(bytes == null || bytes.length < 12)
      {
         return (null);
      }
      if((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G' && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A)
      {
         return ("png");
      }
      if((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF)
      {
         return ("jpeg");
      }
      if(bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a')
      {
         return ("gif");
      }
      if(bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P')
      {
         return ("webp");
      }
      return (null);
   }



   /*******************************************************************************
    ** Validate and convert: PNG bytes for each of UserProfilesMetaData.AVATAR_SIZES
    ** (keyed by size, in ascending order).
    *******************************************************************************/
   public static Map<Integer, byte[]> process(byte[] bytes) throws QBadRequestException
   {
      return (process(bytes, DECODES));
   }



   /*******************************************************************************
    ** Validate and convert under a given set of decode permits (uploads and the
    ** Gravatar proxy each have their own, so neither can starve the other).
    *******************************************************************************/
   static Map<Integer, byte[]> process(byte[] bytes, Semaphore permits) throws QBadRequestException
   {
      String format = detectFormat(bytes);
      if(format == null)
      {
         throw (new QBadRequestException(REFUSED_TYPE));
      }

      boolean acquired = false;
      try
      {
         acquired = permits.tryAcquire(10, TimeUnit.SECONDS);
         if(!acquired)
         {
            throw (new QBadRequestException("Photos are busy being processed. Try again in a moment."));
         }
         return (convert(bytes, format));
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw (new QBadRequestException("Photos are busy being processed. Try again in a moment."));
      }
      finally
      {
         if(acquired)
         {
            permits.release();
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<Integer, byte[]> convert(byte[] bytes, String format) throws QBadRequestException
   {
      BufferedImage square = decodeSquare(bytes, format);
      BufferedImage work   = scale(square, WORK_EDGE);
      if("jpeg".equals(format))
      {
         work = orient(work, exifOrientation(bytes));
      }

      Map<Integer, byte[]> rs = new LinkedHashMap<>();
      for(Integer size : UserProfilesMetaData.AVATAR_SIZES)
      {
         rs.put(size, writePng(size == WORK_EDGE ? work : scale(work, size)));
      }
      return (rs);
   }



   /*******************************************************************************
    ** Decode only the centred square, subsampled for large images, after checking
    ** the dimensions from the header.
    *******************************************************************************/
   private static BufferedImage decodeSquare(byte[] bytes, String format) throws QBadRequestException
   {
      ImageReader reader = findReader(format);
      try(ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes)))
      {
         reader.setInput(input, true, true);
         int width  = reader.getWidth(0);
         int height = reader.getHeight(0);
         if(width < 1 || height < 1 || width > MAX_EDGE || height > MAX_EDGE || (long) width * height > UserProfilesMetaData.MAX_PIXELS)
         {
            throw (new QBadRequestException("That image's dimensions are too large: use one under 40 megapixels."));
         }

         int            edge  = Math.min(width, height);
         ImageReadParam param = reader.getDefaultReadParam();
         param.setSourceRegion(new Rectangle((width - edge) / 2, (height - edge) / 2, edge, edge));
         int subsampling = Math.max(1, edge / DECODE_CAP);
         param.setSourceSubsampling(subsampling, subsampling, 0, 0);
         BufferedImage image = reader.read(0, param);
         if(image == null)
         {
            throw (new QBadRequestException("That image couldn't be read. " + REFUSED_TYPE));
         }
         return (image);
      }
      catch(QBadRequestException e)
      {
         throw (e);
      }
      catch(Exception | OutOfMemoryError e)
      {
         throw (new QBadRequestException("That image couldn't be read. " + REFUSED_TYPE));
      }
      finally
      {
         reader.dispose();
      }
   }



   /*******************************************************************************
    ** A reader for the format (WebP comes from the TwelveMonkeys plugin, which a
    ** shaded or nested classloader may only find after a rescan).
    *******************************************************************************/
   private static ImageReader findReader(String format) throws QBadRequestException
   {
      Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format);
      if(!readers.hasNext())
      {
         ImageIO.scanForPlugins();
         readers = ImageIO.getImageReadersByFormatName(format);
      }
      if(!readers.hasNext())
      {
         throw (new QBadRequestException(REFUSED_TYPE));
      }
      return (readers.next());
   }



   /*******************************************************************************
    ** Scale a square image to edge x edge (halving steps, then bicubic), as ARGB.
    *******************************************************************************/
   static BufferedImage scale(BufferedImage source, int edge)
   {
      BufferedImage current = toArgb(source);
      while(current.getWidth() / 2 >= edge)
      {
         current = draw(current, current.getWidth() / 2, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      }
      return (current.getWidth() == edge && current.getHeight() == edge ? current : draw(current, edge, RenderingHints.VALUE_INTERPOLATION_BICUBIC));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static BufferedImage draw(BufferedImage source, int edge, Object interpolation)
   {
      BufferedImage target   = new BufferedImage(edge, edge, BufferedImage.TYPE_INT_ARGB);
      Graphics2D    graphics = target.createGraphics();
      try
      {
         graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
         graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
         graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         graphics.drawImage(source, 0, 0, edge, edge, null);
      }
      finally
      {
         graphics.dispose();
      }
      return (target);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static BufferedImage toArgb(BufferedImage source)
   {
      if(source.getType() == BufferedImage.TYPE_INT_ARGB)
      {
         return (source);
      }
      BufferedImage target   = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
      Graphics2D    graphics = target.createGraphics();
      try
      {
         graphics.drawImage(source, 0, 0, null);
      }
      finally
      {
         graphics.dispose();
      }
      return (target);
   }



   /*******************************************************************************
    ** Encode as PNG; ImageIO writes no metadata from the source.
    *******************************************************************************/
   private static byte[] writePng(BufferedImage image) throws QBadRequestException
   {
      ImageWriter           writer = ImageIO.getImageWritersByFormatName("png").next();
      ByteArrayOutputStream out    = new ByteArrayOutputStream();
      try(MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(out))
      {
         writer.setOutput(output);
         writer.write(image);
      }
      catch(IOException e)
      {
         throw (new QBadRequestException("That image couldn't be converted."));
      }
      finally
      {
         writer.dispose();
      }
      return (out.toByteArray());
   }



   /*******************************************************************************
    ** Apply an EXIF orientation (1-8) to a square image.
    *******************************************************************************/
   static BufferedImage orient(BufferedImage image, int orientation)
   {
      if(orientation < 2 || orientation > 8)
      {
         return (image);
      }
      int           n      = image.getWidth();
      BufferedImage target = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
      for(int y = 0; y < n; y++)
      {
         for(int x = 0; x < n; x++)
         {
            int tx;
            int ty;
            switch(orientation)
            {
               case 2 ->
               {
                  tx = n - 1 - x;
                  ty = y;
               }
               case 3 ->
               {
                  tx = n - 1 - x;
                  ty = n - 1 - y;
               }
               case 4 ->
               {
                  tx = x;
                  ty = n - 1 - y;
               }
               case 5 ->
               {
                  tx = y;
                  ty = x;
               }
               case 6 ->
               {
                  tx = n - 1 - y;
                  ty = x;
               }
               case 7 ->
               {
                  tx = n - 1 - y;
                  ty = n - 1 - x;
               }
               default ->
               {
                  tx = y;
                  ty = n - 1 - x;
               }
            }
            target.setRGB(tx, ty, image.getRGB(x, y));
         }
      }
      return (target);
   }



   /*******************************************************************************
    ** The EXIF orientation of a JPEG (1 when absent or unreadable).  Reads the
    ** APP1 Exif segment's first IFD only.
    *******************************************************************************/
   static int exifOrientation(byte[] jpeg)
   {
      try
      {
         int position = 2;
         while(position + 4 <= jpeg.length && (jpeg[position] & 0xFF) == 0xFF)
         {
            int marker  = jpeg[position + 1] & 0xFF;
            int length  = ((jpeg[position + 2] & 0xFF) << 8) | (jpeg[position + 3] & 0xFF);
            int segment = position + 4;
            if(marker == 0xDA || marker == 0xD9 || length < 2)
            {
               return (1);
            }
            if(marker == 0xE1 && segment + 6 <= jpeg.length && jpeg[segment] == 'E' && jpeg[segment + 1] == 'x' && jpeg[segment + 2] == 'i' && jpeg[segment + 3] == 'f')
            {
               return (tiffOrientation(jpeg, segment + 6, position + 2 + length));
            }
            position += 2 + length;
         }
      }
      catch(RuntimeException e)
      {
         //////////////////////////////////////////////////////////////
         // malformed metadata only loses the rotation, never fails  //
         //////////////////////////////////////////////////////////////
      }
      return (1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static int tiffOrientation(byte[] bytes, int tiff, int end)
   {
      boolean little = bytes[tiff] == 'I' && bytes[tiff + 1] == 'I';
      int     ifd    = tiff + readInt(bytes, tiff + 4, little);
      int     count  = readShort(bytes, ifd, little);
      for(int i = 0; i < count; i++)
      {
         int entry = ifd + 2 + i * 12;
         if(entry + 12 > end)
         {
            break;
         }
         if(readShort(bytes, entry, little) == 0x0112)
         {
            return (readShort(bytes, entry + 8, little));
         }
      }
      return (1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static int readShort(byte[] bytes, int at, boolean little)
   {
      int a = bytes[at] & 0xFF;
      int b = bytes[at + 1] & 0xFF;
      return (little ? (b << 8) | a : (a << 8) | b);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static int readInt(byte[] bytes, int at, boolean little)
   {
      return (little
         ? (readShort(bytes, at + 2, true) << 16) | readShort(bytes, at, true)
         : (readShort(bytes, at, false) << 16) | readShort(bytes, at + 2, false));
   }
}
