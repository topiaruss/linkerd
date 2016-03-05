package io.buoyant.linkerd.admin

import com.twitter.finagle.Service
import com.twitter.finagle.http.{MediaType, Request, Response}
import com.twitter.util.Future
import io.buoyant.linkerd.Linker.LinkerConfig
import io.buoyant.linkerd.config.Parser

private[admin] class ConfigHandler(config: LinkerConfig)
  extends Service[Request, Response] {

  val mapper = Parser.jsonObjectMapper

  override def apply(request: Request): Future[Response] =
    AdminHandler.mkResponse(mapper.writeValueAsString(config), MediaType.Json)
}
