package io.buoyant.linkerd.config.types

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{SerializerProvider, DeserializationContext}
import com.twitter.finagle.thrift.Protocols
import io.buoyant.linkerd.config.{ConfigSerializer, ConfigDeserializer}
import org.apache.thrift.protocol.{TBinaryProtocol, TCompactProtocol, TProtocolFactory}

class ThriftProtocolDeserializer extends ConfigDeserializer[TProtocolFactory] {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): TProtocolFactory =
    catchMappingException(ctxt) {
      _parseString(jp, ctxt) match {
        case "binary" => Protocols.binaryFactory()
        case "compact" => new TCompactProtocol.Factory()
        case protocol =>
          throw new IllegalArgumentException(s"unsupported thrift protocol $protocol")
      }
    }
}

class ThriftProtocolSerializer extends ConfigSerializer[TProtocolFactory] {
  override def serialize(
    value: TProtocolFactory,
    jgen: JsonGenerator,
    provider: SerializerProvider
  ): Unit = {
    val protocol = value match {
      case _: TBinaryProtocol.Factory => "binary"
      case _: TCompactProtocol.Factory => "compact"
      case _ => "unknown"
    }
    jgen.writeString(protocol)
  }
}
