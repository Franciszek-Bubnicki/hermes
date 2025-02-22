package pl.allegro.tech.hermes.consumers.consumer.converter;

import static com.google.common.collect.ImmutableMap.of;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static pl.allegro.tech.hermes.consumers.consumer.Message.message;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

@RunWith(MockitoJUnitRunner.class)
public class AvroToJsonMessageConverterTest {

  @Test
  public void shouldConvertToJsonWithoutMetadata() throws IOException {
    // given
    Topic topic = topic("group.topic").build();
    AvroUser avroUser = new AvroUser("Bob", 18, "blue");
    Message source =
        message()
            .withData(avroUser.asBytes())
            .withSchema(CompiledSchema.of(avroUser.getSchema(), 1, 0))
            .withExternalMetadata(of())
            .build();
    AvroToJsonMessageConverter converter = new AvroToJsonMessageConverter();

    // when
    Message target = converter.convert(source, topic);

    // then
    assertThatJson(new String(target.getData()))
        .isEqualTo("{\"name\": \"Bob\", \"age\": 18, \"favoriteColor\": \"blue\"}");
  }
}
