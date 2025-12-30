/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.middleware.health.indicators;


import java.io.File;
import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.health.HealthIndicator;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Health indicator that checks available disk space.
 **
 ** This indicator:
 ** - Checks free space at a specified path
 ** - Returns DOWN if below minimum threshold
 ** - Returns DEGRADED if space is low but above minimum
 ** - Configurable path and minimum free bytes
 *******************************************************************************/
public class DiskSpaceHealthIndicator implements HealthIndicator
{
   private static final QLogger LOG = QLogger.getLogger(DiskSpaceHealthIndicator.class);

   private String path             = "/";
   private Long   minimumFreeBytes = 1_000_000_000L; // Default 1GB



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public DiskSpaceHealthIndicator()
   {
   }



   /*******************************************************************************
    ** Get the name of this health indicator
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ("diskSpace");
   }



   /*******************************************************************************
    ** Execute the disk space health check
    *******************************************************************************/
   @Override
   public HealthCheckResult check(QInstance qInstance) throws QException
   {
      long startTime = System.currentTimeMillis();

      if(path == null)
      {
         return new HealthCheckResult()
            .withStatus(HealthStatus.UNKNOWN)
            .withDurationMs(System.currentTimeMillis() - startTime)
            .withDetail("error", "No path configured");
      }

      try
      {
         File file = new File(path);

         if(!file.exists())
         {
            return new HealthCheckResult()
               .withStatus(HealthStatus.DOWN)
               .withDurationMs(System.currentTimeMillis() - startTime)
               .withDetail("error", "Path does not exist")
               .withDetail("path", path);
         }

         long freeSpace  = file.getFreeSpace();
         long totalSpace = file.getTotalSpace();
         long usedSpace  = totalSpace - freeSpace;

         Map<String, Object> details = new HashMap<>();
         details.put("path", path);
         details.put("totalBytes", totalSpace);
         details.put("freeBytes", freeSpace);
         details.put("usedBytes", usedSpace);
         details.put("minimumFreeBytes", minimumFreeBytes);

         //////////////////////////////////////////
         // Determine status based on free space //
         //////////////////////////////////////////
         HealthStatus status;
         if(freeSpace < minimumFreeBytes)
         {
            status = HealthStatus.DOWN;
            details.put("error", "Free space below minimum threshold");
         }
         else if(freeSpace < (minimumFreeBytes * 2))
         {
            status = HealthStatus.DEGRADED;
            details.put("warning", "Free space is low");
         }
         else
         {
            status = HealthStatus.UP;
         }

         return new HealthCheckResult()
            .withStatus(status)
            .withDurationMs(System.currentTimeMillis() - startTime)
            .withDetails(details);
      }
      catch(Exception e)
      {
         LOG.warn("Disk space health check failed", logPair("path", path), e);
         return new HealthCheckResult()
            .withStatus(HealthStatus.UNKNOWN)
            .withDurationMs(System.currentTimeMillis() - startTime)
            .withDetail("error", e.getMessage())
            .withDetail("exceptionType", e.getClass().getSimpleName());
      }
   }



   /*******************************************************************************
    ** Fluent setter for path
    *******************************************************************************/
   public DiskSpaceHealthIndicator withPath(String path)
   {
      this.path = path;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for minimumFreeBytes
    *******************************************************************************/
   public DiskSpaceHealthIndicator withMinimumFreeBytes(Long minimumFreeBytes)
   {
      this.minimumFreeBytes = minimumFreeBytes;
      return (this);
   }



   /*******************************************************************************
    ** Getter for path
    *******************************************************************************/
   public String getPath()
   {
      return (this.path);
   }



   /*******************************************************************************
    ** Setter for path
    *******************************************************************************/
   public void setPath(String path)
   {
      this.path = path;
   }



   /*******************************************************************************
    ** Getter for minimumFreeBytes
    *******************************************************************************/
   public Long getMinimumFreeBytes()
   {
      return (this.minimumFreeBytes);
   }



   /*******************************************************************************
    ** Setter for minimumFreeBytes
    *******************************************************************************/
   public void setMinimumFreeBytes(Long minimumFreeBytes)
   {
      this.minimumFreeBytes = minimumFreeBytes;
   }
}
