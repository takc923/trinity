package org.sisioh.trinity.test

import org.sisioh.trinity.application.TrinityApplication
import org.specs2.mutable.Specification
import org.sisioh.trinity.domain.controller.SimpleController
import org.sisioh.trinity.domain.config.Config
import org.sisioh.trinity.view.scalate.{ScalateEngineContext, ScalateRenderer}


class TestController(implicit application: TrinityApplication) extends SimpleController {

  get("/hey") {
    request =>
      responseBuilder.withPlain("hello").withOk.toFinagleResponse
  }

  //  class TestView extends MustacheView {
  //    def template: String = "test_view.mustache"
  //
  //    val test_val = "aaaa"
  //  }


  //  get("/test") {
  //    request =>
  //      render.withView(new TestView).toFuture
  //  }

  get("/scalate") {
    request =>
      val bindings = Map(
        "name" -> "Scalate",
        "languages" -> List("Java", "Scala", "Clojure", "Groovy")
      )
      implicit val scalate = ScalateEngineContext()
      responseBuilder.withBodyRenderer(ScalateRenderer("scalate_test.ssp", bindings)).toFinagleResponse
  }


  /**
   * Uploading files
   *
   * curl -F avatar=@/path/to/img http://localhost:7070/profile
   */
  post("/profile") {
    request =>
      request.multiParams.get("avatar").map {
        avatar =>
          println("content type is " + avatar.contentType)
          avatar.writeToFile("/tmp/avatar") //writes uploaded avatar to /tmp/avatar
      }
      responseBuilder.withPlain("ok").toFinagleResponse
  }


}

class ServerSpec extends Specification {

  val config = Config()

  "app" should {
    "register" in {
      implicit val s = TrinityApplication(config)
      val controller = new TestController
      s.registerController(controller)
      s.start()
    }
  }

}
