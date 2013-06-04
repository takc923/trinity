package org.sisioh.trinity

import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import com.twitter.finagle.http.{Response => FinagleResponse, Request => FinagleRequest}
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.util.Future
import org.sisioh.scala.toolbox.LoggingEx
import org.json4s.jackson.Json
import org.json4s.DefaultFormats

object Response {
  def apply(body: String) = new Response().body(body).status(200).build

  def apply(status: Int, body: String) = new Response().body(body).status(status).build

  def apply(status: Int, body: String, headers: Map[String, String]) = new Response().body(body).status(status).headers(headers).build
}

class Response extends LoggingEx {
  var status: Int = 200

  var headers: Map[String, String] = Map()

  var hasCookies = false
  lazy val cookies = new CookieEncoder(true)

  var strBody: Option[String] = None
  var binBody: Option[Array[Byte]] = None
  var json: Option[Any] = None
  var view: Option[View] = None

  def setContent(resp: HttpResponse): Unit = {
    json match {
      case Some(j) =>
        resp.setHeader("Content-Type", "application/json")
        val json = Json(DefaultFormats).write(j.asInstanceOf[AnyRef])
        resp.setContent(copiedBuffer(json, UTF_8))
      case None =>
        view match {
          case Some(v) =>
            val out = v.render
            resp.setContent(copiedBuffer(out, UTF_8))
          case None =>
            strBody match {
              case Some(sb) =>
                resp.setContent(copiedBuffer(sb, UTF_8))
              case None =>
                binBody match {
                  case Some(bb) =>
                    resp.setContent(copiedBuffer(bb))
                  case None =>
                    throw new RuntimeException("nothing to render")
                }
            }
        }
    }
  }

  def cookie(k: String, v: String) = {
    this.hasCookies = true
    this.cookies.addCookie(k, v)
    this
  }

  def cookie(c: Cookie) = {
    this.hasCookies = true
    this.cookies.addCookie(c)
    this
  }

  def ok = {
    status(200)
    this
  }

  def notFound = {
    status(404)
    this
  }

  def body(s: String) = {
    this.strBody = Some(s)
    this
  }

  def status(i: Int): Response = {
    this.status = i
    this
  }

  def nothing = {
    this.header("Content-Type", "text/plain")
    this.body("")
    this
  }

  def plain(body: String) = {
    this.header("Content-Type", "text/plain")
    this.body(body)
    this
  }

  def html(body: String) = {
    this.header("Content-Type", "text/html")
    this.body(body)
    this
  }

  def body(b: Array[Byte]) = {
    this.binBody = Some(b)
    this
  }

  def header(k: String, v: String) = {
    this.headers += (k -> v)
    this
  }

  def headers(m: Map[String, String]): Response = {
    this.headers = this.headers ++ m
    this
  }

  def json(o: Any): Response = {
    this.header("Content-Type", "application/json")
    this.json = Some(o)
    this
  }

  def view(v: View): Response = {
    this.view = Some(v)
    this
  }

  def build: FinagleResponse = {
    val responseStatus = HttpResponseStatus.valueOf(status)
    val resp = new DefaultHttpResponse(HTTP_1_1, responseStatus)

    headers.foreach {
      xs =>
        resp.setHeader(xs._1, xs._2)
    }

    if (this.hasCookies) resp.setHeader("Set-Cookie", cookies.encode)

    setContent(resp)
    FinagleResponse(resp)
  }

  def toFuture = {
    Future.value(this)
  }

  override def toString = {
    val buf = new StringBuilder
    buf.append(getClass().getSimpleName())
    buf.append('\n')
    buf.append(HTTP_1_1.toString)
    buf.append(' ')
    buf.append(this.status)
    buf.append('\n')
    appendCollection[String, String](buf, this.headers)

    buf.toString
  }

  def appendCollection[A, B](buf: StringBuilder, x: Map[A, B]) {
    x foreach {
      xs =>
        buf.append(xs._1)
        buf.append(" : ")
        buf.append(xs._2)
    }
  }
}