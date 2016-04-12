package io.buoyant.namerd

import com.twitter.finagle.Dtab
import com.twitter.finagle.serverset2.ZkClient
import com.twitter.io.Buf
import com.twitter.util.Await
import io.buoyant.admin.App
import io.buoyant.config.Parser
import io.buoyant.namerd.storage.experimental.{ZkDtabStoreInitializer, zk}
import java.io.File
import scala.io.Source

object DcosBootstrap extends App {

  val defaultNs = "default"
  val defaultDtab = Dtab.read(
    """|/srv        => /io.l5d.marathon;
       |/www        => /srv/web;
       |/host       => /srv | /$/io.buoyant.http.anyHostPfx/www;
       |/method     => /$/io.buoyant.http.anyMethodPfx/host;
       |/http/1.1   => /method;
       |/http/1.0   => /$/io.buoyant.http.anyMethodPfx/www;
       |""".stripMargin
  )

  def main(): Unit = {
    args match {
      case Array(path) =>
        val config = loadZkConfig(path)

        val zkClient = new ZkClient(
          config.hosts.mkString(","),
          config.pathPrefix.getOrElse("/dtabs"),
          config.sessionTimeout
        )

        Await.result(zkClient.create(defaultNs, Buf.Utf8(defaultDtab.show)))

      case _ => exitOnError("usage: namerd-dcos-bootstrap path/to/config")
    }
  }

  private def loadZkConfig(path: String): zk = {
    val configText = path match {
      case "-" =>
        Source.fromInputStream(System.in).mkString
      case path =>
        val f = new File(path)
        if (!f.isFile) throw new IllegalArgumentException(s"config is not a file: $path")
        Source.fromFile(f).mkString
    }
    val mapper = Parser.objectMapper(configText, Seq(Seq(new ZkDtabStoreInitializer)))
    val root = mapper.readTree(configText)
    val storage = root.findValue("storage")
    val kind = storage.get("kind").textValue
    if (kind != "io.buoyant.namerd.storage.experimental.zk")
      exitOnError(s"config file does not specify zk storage: $kind")
    mapper.treeToValue[zk](storage)
  }
}
