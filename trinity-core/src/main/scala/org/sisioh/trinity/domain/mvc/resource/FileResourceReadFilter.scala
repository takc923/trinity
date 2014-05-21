/*
 * Copyright 2013 Sisioh Project and others. (http://sisioh.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.sisioh.trinity.domain.mvc.resource

import java.io.{InputStream, File}
import org.apache.commons.io.IOUtils
import org.sisioh.trinity.domain.io.buffer.ChannelBuffers
import org.sisioh.trinity.domain.io.http.MimeTypes
import org.sisioh.trinity.domain.mvc.Environment
import org.sisioh.trinity.domain.mvc.action.Action
import org.sisioh.trinity.domain.mvc.filter.SimpleFilter
import org.sisioh.trinity.domain.mvc.http.{ResponseSupport, Request, Response}
import scala.concurrent.Future
import scala.util.Try

/**
 * Represents the filter to read the file resources.
 *
 * @param environment [[Environment.Value]]
 * @param localBasePath local base path
 */
class FileResourceReadFilter(environment: Environment.Value, localBasePath: File)
  extends SimpleFilter[Request, Response] with ResponseSupport {

  private val fileResolver = FileResourceResolver(environment, localBasePath)

  def isValidPath(path: String): Boolean = {
    var fi: InputStream = null
    try {
      fi = getClass.getResourceAsStream(path)
      if (fi != null && fi.available > 0) true else false
    } catch {
      case e: Exception => false
    } finally {
      if (fi != null) fi.close()
    }
  }


  def apply(requestIn: Request, action: Action[Request, Response]): Future[Response] = {
    if (fileResolver.hasFile(requestIn.uri) && requestIn.uri != '/') {
      fileResolver.getInputStream(requestIn.uri).flatMap {
        fh =>
          try {
            for {
              bytes <- Try(IOUtils.toByteArray(fh))
              result <- Try(fh.read(bytes))
            } yield {
              val mimeType = MimeTypes.fileExtensionOf('.' + requestIn.uri.toString.split('.').last)
              responseBuilder.withContentType(mimeType).withContent(ChannelBuffers.copiedBuffer(bytes)).toFuture
            }
          } finally {
            fh.close()
          }
      }.get
    } else {
      action(requestIn)
    }
  }
}
