package io.l5d.experimental

import com.twitter.finagle.util.LoadService
import com.twitter.finagle.{Path, Stack}
import io.buoyant.config.Parser
import io.buoyant.config.types.Port
import io.buoyant.namer.{NamerConfig, NamerInitializer}
import org.scalatest.FunSuite

class MarathonTest extends FunSuite {

  test("sanity") {
    // ensure it doesn't totally blowup
    marathon(None, None, None, None).newNamer(Stack.Params.empty)
  }

  test("service registration") {
    assert(LoadService[NamerInitializer]().exists(_.isInstanceOf[MarathonInitializer]))
  }

  test("parse config") {
    val yaml = s"""
                  |kind:      io.l5d.experimental.marathon
                  |prefix:    /io.l5d.marathon
                  |host:      marathon.mesos
                  |port:      80
                  |uriPrefix: /marathon
                  |ttlMs:     300
      """.stripMargin

    val mapper = Parser.objectMapper(yaml, Iterable(Seq(MarathonInitializer)))
    val marathon = mapper.readValue[NamerConfig](yaml).asInstanceOf[marathon]
    assert(marathon.host.contains("marathon.mesos"))
    assert(marathon.port.contains(Port(80)))
    assert(marathon.uriPrefix.contains("/marathon"))
    assert(marathon._prefix.contains(Path.read("/io.l5d.marathon")))
    assert(marathon.ttlMs.contains(300))
  }
}
