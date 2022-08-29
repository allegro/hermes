package pl.allegro.tech.hermes.management.domain.topic.schema

import org.apache.avro.compiler.idl.ParseException
import spock.lang.Specification

import static org.apache.commons.lang.StringUtils.deleteWhitespace
import static pl.allegro.tech.hermes.management.domain.topic.schema.SchemaConverter.convertToAvroSchema

class SchemaConverterTest extends Specification {

    def "should convert valid AVRO IDL to AVRO Schema"() {
        given:
        String avroIdl = """@namespace("pl.local")
                         protocol TopicA {
                           record TopicA {
                             union{null, map<string>} __metadata = null;
                             NestedObject nestedObject;
                           }
                           record NestedObject {
                             string id;
                             string field1;
                             string field2;
                           }
                         }""".stripIndent()

        String expectedSchema = deleteWhitespace((
                """{
                     "type": "record",
                     "name": "TopicA",
                     "namespace": "pl.local",
                     "fields": [
                       {
                         "name": "__metadata",
                         "type": [
                           "null",
                           {
                             "type": "map",
                             "values": "string"
                           }
                         ],
                         "default": null
                       },
                       {
                         "name": "nestedObject",
                         "type": {
                           "type": "record",
                           "name": "NestedObject",
                           "fields": [
                             {
                               "name": "id",
                               "type": "string"
                             },
                             {
                               "name": "field1",
                               "type": "string"
                             },
                             {
                               "name": "field2",
                               "type": "string"
                             }
                           ]
                         }
                       }
                     ]
                   }"""))

        when:
        def convertedSchema = convertToAvroSchema(avroIdl)

        then:
        convertedSchema == expectedSchema
    }

    def "should convert valid AVRO IDL to AVRO Schema with comments"() {
        given:
        String avroIdl = """@namespace("pl.local")
                         protocol TopicA {
                           record TopicA {
                             /** Field used in Hermes internals to propagate metadata */
                             union{null, map<string>} __metadata = null;
                             /** NestedObject description */
                             NestedObject nestedObject;
                           }
                           record NestedObject {
                             string id;
                             /** field1 description */
                             string field1;
                             string field2;
                           }
                         }""".stripIndent()

        when:
        def convertedSchema = convertToAvroSchema(avroIdl)

        then:
        convertedSchema.contains("Field used in Hermes internals to propagate metadata")
        convertedSchema.contains("NestedObject description")
        convertedSchema.contains("field1 description")
    }

    def "should throw exception when converting invalid AVRO IDL - missing { character"() {
        given:
        String avroIdl = """@namespace("pl.local")
                         protocol TopicA {
                           record TopicA 
                             union{null, map<string>} __metadata = null;
                             NestedObject nestedObject;
                           }
                           record NestedObject {
                             string id;
                             string field1;
                             string field2;
                           }
                         }""".stripIndent()

        when:
        convertToAvroSchema(avroIdl)

        then:
        def e = thrown ParseException
        e.message.contains("Encountered \" \"union")
        e.message.contains("Was expecting:\n    \"{\" ...")
    }

    def "should throw exception when converting invalid AVRO IDL - missing @ character before namespace"() {
        given:
        String avroIdl = """namespace("pl.local")
                         protocol TopicA {
                           record TopicA 
                             union{null, map<string>} __metadata = null;
                             NestedObject nestedObject;
                           }
                           record NestedObject {
                             string id;
                             string field1;
                             string field2;
                           }
                         }""".stripIndent()

        when:
        convertToAvroSchema(avroIdl)

        then:
        def e = thrown ParseException
        e.message.contains("Encountered \" <IDENTIFIER> \"namespace")
        e.message.contains("Was expecting one of:\n    \"protocol\" ...\n    \"@\" ...")
    }

    def "should throw exception when converting invalid AVRO IDL - missing protocol section"() {
        given:
        String avroIdl = """@namespace("pl.local")
                           record TopicA 
                             union{null, map<string>} __metadata = null;
                             NestedObject nestedObject;
                           record NestedObject {
                             string id;
                             string field1;
                             string field2;
                           }
                         }""".stripIndent()

        when:
        convertToAvroSchema(avroIdl)

        then:
        def e = thrown ParseException
        e.message.contains("Encountered \" \"record")
        e.message.contains("Was expecting one of:\n    \"protocol\" ...\n")
    }

}
