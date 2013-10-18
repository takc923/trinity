package org.sisioh.trinity.domain.io.http

import scala.collection.JavaConverters.asScalaSetConverter

import org.jboss.netty.handler.codec.http.CookieDecoder
import org.jboss.netty.handler.codec.http.CookieEncoder
import org.jboss.netty.handler.codec.http.{ HttpMessage => NettyMessage }
import org.sisioh.trinity.domain.io.buffer.ChannelBuffer
import org.sisioh.trinity.domain.io.http.Cookie.toNetty
import org.sisioh.trinity.domain.io.http.Cookie.toTrinity

/**
 * HTTPのメッセージを表すトレイト。
 */
trait Message {

  val netty: NettyMessage

  def isRequest: Boolean

  def isResponse = !isRequest

  def getHeader(name: String): String

  def getHeaders(name: String): Seq[String]

  def headers: Seq[(String, Any)]

  def containsHeader(name: String): Boolean

  def headerNames: Set[String]

  def protocolVersion: Version.Value

  def withProtocolVersion(version: Version.Value): this.type

  def content: ChannelBuffer

  def withContent(content: ChannelBuffer): this.type

  def withHeader(name: String, value: Any): this.type

  def withHeader(name: String, values: Seq[_]): this.type

  def withHeaders(headers: Seq[(String, Any)]): this.type = {
    headers.foldLeft(this) {
      (l, r) =>
        l.withHeader(r._1, r._2)
    }.asInstanceOf[this.type]
  }

  def withoutHeader(name: String): this.type

  def withoutAllHeaders: this.type

  def isChunked: Boolean

  def withChunked(chunked: Boolean): this.type

  private val cookieHeaderName = if (isResponse) "Set-Cookie" else "Cookie"

  def withCookies(cookies: Seq[Cookie]): this.type = {
    val cookieEncoder = new CookieEncoder(true)
    cookies.foreach {
      xs =>
        cookieEncoder.addCookie(xs)
    }
    withHeader(cookieHeaderName, cookieEncoder.encode)
  }

  def cookies: Seq[Cookie] = {
    val decoder = new CookieDecoder()
    val header = getHeader(cookieHeaderName)
    decoder.decode(header).asScala.map(toTrinity).toSeq
  }

  override def toString = netty.toString

}