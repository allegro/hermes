package pl.allegro.tech.hermes.consumers;

import org.apache.avro.generic.GenericRecord;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import scala.collection.immutable.List;
import wandou.avpath.Evaluator;
import wandou.avpath.Parser;

import static org.assertj.core.api.Assertions.assertThat;

public class AvPathTest {


    @Test
    public void shouldXXX() {
        // given
        AvroUser user = new AvroUser("foo name", 100, "black");
        GenericRecord record = user.getRecord();
        Parser.PathSyntax ast = new Parser().parse(".name");

        // when
        List<Evaluator.Ctx> select = Evaluator.select(record, ast);

        // then
        Evaluator.Ctx head = select.head();
        assertThat(head.value()).isEqualTo("foo name");
    }

}
