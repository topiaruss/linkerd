package io.l5d

import com.twitter.finagle.util.LoadService
import com.twitter.finagle.{Path, Stack}
import io.buoyant.config.Parser
import io.buoyant.namer.{InterpreterConfig, InterpreterInitializer}
import org.scalatest.FunSuite

class NamerdTest extends FunSuite {
  test("sanity") {
    // ensure it doesn't totally blowup
    namerd(Some(Path.read("/whats/in/a")), Some("name"), None).newInterpreter(Stack.Params.empty)
  }

  test("interpreter registration") {
    assert(LoadService[InterpreterInitializer]().exists(_.isInstanceOf[NamerdInterpreterInitializer]))
  }

  test("parse config") {
    val yaml = s"""|kind: io.l5d.namerd
                   |dst: /$$/inet/127.1/4100
                   |namespace: name
                   |""".stripMargin

    val mapper = Parser.objectMapper(yaml, Iterable(Seq(NamerdInterpreterInitializer)))
    val namerd = mapper.readValue[InterpreterConfig](yaml).asInstanceOf[namerd]
    assert(namerd.dst == Some(Path.read("/$/inet/127.1/4100")))
    assert(namerd.namespace == Some("name"))
  }
}
