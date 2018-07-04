package pl.allegro.tech.hermes.management.domain.topic;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.group.GroupService;

import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class TopicOwnerCache {

    private static final Logger logger = LoggerFactory.getLogger(TopicOwnerCache.class);

    private final TopicRepository topicRepository;
    private final GroupService groupService;
    private final ScheduledExecutorService scheduledExecutorService;

    private final Multimap<OwnerId, TopicName> cache = Multimaps.synchronizedMultimap(ArrayListMultimap.create());

    public TopicOwnerCache(TopicRepository topicRepository, GroupService groupService,
                           @Value("${topicOwnerCache.refreshRateInSeconds}") int refreshRateInSeconds) {
        this.topicRepository = topicRepository;
        this.groupService = groupService;
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("topic-owner-cache-%d")
                        .build());
        scheduledExecutorService.scheduleAtFixedRate(this::fillCache, 0, refreshRateInSeconds, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        scheduledExecutorService.shutdown();
    }

    public Collection<TopicName> get(OwnerId ownerId) {
        return cache.get(ownerId);
    }

    private void fillCache() {
        logger.info("Starting filling Owner Id to Topic Name cache");
        long start = System.currentTimeMillis();
        groupService.listGroupNames().stream()
                .flatMap(groupName -> topicRepository.listTopics(groupName).stream())
                .forEach(topic -> cache.put(topic.getOwner(), topic.getName()));
        long end = System.currentTimeMillis();
        logger.info("Cache filled. Took {}ms", end - start);
    }

    void onRemovedTopic(Topic topic) {
        cache.remove(topic.getOwner(), topic.getName());
    }

    void onCreatedTopic(Topic topic) {
        cache.put(topic.getOwner(), topic.getName());
    }

    void onUpdatedTopic(Topic oldTopic, Topic newTopic) {
        cache.remove(oldTopic.getOwner(), oldTopic.getName());
        cache.put(newTopic.getOwner(), newTopic.getName());
    }
}
