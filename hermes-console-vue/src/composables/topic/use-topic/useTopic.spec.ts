import {afterEach, describe, expect} from 'vitest';
import {
    dummySubscription,
    secondDummySubscription,
} from '@/dummy/subscription';
import {
    dummyTopic,
    dummyTopicMessagesPreview,
    dummyTopicMetrics,
    dummyTopicOwner,
} from '@/dummy/topic';
import {
    fetchTopicErrorHandler,
    fetchTopicMessagesPreviewErrorHandler,
    fetchTopicMetricsErrorHandler,
    fetchTopicOwnerErrorHandler,
    fetchTopicSubscriptionDetailsErrorHandler,
    fetchTopicSubscriptionsErrorHandler,
    successfulTopicHandlers,
} from '@/mocks/handlers';
import {setupServer} from 'msw/node';
import {useTopic} from '@/composables/topic/use-topic/useTopic';
import type { UseTopicErrors } from "@/composables/topic/use-topic/useTopic";

describe('useTopic', () => {
    const server = setupServer(...successfulTopicHandlers);

    const topicName = dummyTopic.name;
    const topicOwner = dummyTopicOwner.id;

    afterEach(() => {
        server.resetHandlers();
    });

    it('should fetch topic', async () => {
        // given
        server.listen();

        // when
        const {
            topic,
            owner,
            metrics,
            messages,
            subscriptions,
            loading,
            error,
            fetchTopic,
        } = useTopic(topicName);
        const topicPromise = fetchTopic();

        // then: loading state was indicated
        expect(topic.value).toBeUndefined();
        expect(owner.value).toBeUndefined();
        expect(metrics.value).toBeUndefined();
        expect(messages.value).toBeUndefined();
        expect(subscriptions.value).toBeUndefined();
        expectNoErrors(error.value);
        expect(loading.value).toBeTruthy();

        // and: correct data was returned
        await topicPromise;
        expect(topic.value).toEqual(dummyTopic);
        expect(owner.value).toEqual(dummyTopicOwner);
        expect(metrics.value).toEqual(dummyTopicMetrics);
        expect(messages.value).toEqual(dummyTopicMessagesPreview);
        expect(subscriptions.value).toEqual([
            dummySubscription,
            secondDummySubscription,
        ]);

        // and: correct loading and error states were indicated
        expect(loading.value).toBeFalsy();
        expectNoErrors(error.value);
    });

    const expectedDataForErrorTest = {
        expectedTopic: dummyTopic,
        expectedOwner: dummyTopicOwner,
        expectedMessages: dummyTopicMessagesPreview,
        expectedMetrics: dummyTopicMetrics,
        expectedSubscriptions: [dummySubscription, secondDummySubscription],
    };

    it.each([
        {
            mockHandler: fetchTopicErrorHandler({topicName}),
            expectedErrors: {fetchTopic: true},
            expectedTopic: undefined,
            expectedOwner: undefined,
            expectedMessages: undefined,
            expectedMetrics: undefined,
            expectedSubscriptions: undefined,
        },
        {
            mockHandler: fetchTopicOwnerErrorHandler({topicOwner}),
            expectedErrors: {fetchOwner: true},
            ...expectedDataForErrorTest,
            expectedOwner: undefined,
        },
        {
            mockHandler: fetchTopicMessagesPreviewErrorHandler({topicName}),
            expectedErrors: {fetchTopicMessagesPreview: true},
            ...expectedDataForErrorTest,
            expectedMessages: undefined,
        },
        {
            mockHandler: fetchTopicMetricsErrorHandler({topicName}),
            expectedErrors: {fetchTopicMetrics: true},
            ...expectedDataForErrorTest,
            expectedMetrics: undefined,
        },
        {
            mockHandler: fetchTopicSubscriptionsErrorHandler({topicName}),
            expectedErrors: {fetchSubscriptions: true},
            ...expectedDataForErrorTest,
            expectedSubscriptions: undefined,
        },
        {
            mockHandler: fetchTopicSubscriptionDetailsErrorHandler({
                topicName,
                subscriptionName: dummySubscription.name,
            }),
            expectedErrors: {fetchSubscriptions: true},
            ...expectedDataForErrorTest,
            expectedSubscriptions: [secondDummySubscription],
        },
        {
            mockHandler: fetchTopicSubscriptionDetailsErrorHandler({
                topicName,
                subscriptionName: secondDummySubscription.name,
            }),
            expectedErrors: {fetchSubscriptions: true},
            ...expectedDataForErrorTest,
            expectedSubscriptions: [dummySubscription],
        },
    ])(
        'should indicate appropriate error',
        async ({
                   mockHandler,
                   expectedErrors,
                   expectedTopic,
                   expectedOwner,
                   expectedMessages,
                   expectedMetrics,
                   expectedSubscriptions,
               }) => {
            // given
            server.use(mockHandler);
            server.listen();

            // when
            const {
                topic,
                owner,
                metrics,
                messages,
                subscriptions,
                loading,
                error,
                fetchTopic,
            } = useTopic(topicName);
            const topicPromise = fetchTopic();

            // then
            await topicPromise;
            expect(topic.value).toEqual(expectedTopic);
            expect(owner.value).toEqual(expectedOwner);
            expect(messages.value).toEqual(expectedMessages);
            expect(metrics.value).toEqual(expectedMetrics);
            expect(subscriptions.value).toEqual(expectedSubscriptions);
            expect(loading.value).toBeFalsy();
            expectErrors(error.value, expectedErrors);
        },
    );
});

function expectErrors(
    errors: UseTopicErrors,
    {
        fetchTopic = false,
        fetchOwner = false,
        fetchTopicMessagesPreview = false,
        fetchTopicMetrics = false,
        fetchSubscriptions = false,
    },
) {
    (fetchTopic && expect(errors.fetchTopic).not.toBeNull()) ||
    expect(errors.fetchTopic).toBeNull();
    (fetchOwner && expect(errors.fetchOwner).not.toBeNull()) ||
    expect(errors.fetchOwner).toBeNull();
    (fetchTopicMessagesPreview &&
        expect(errors.fetchTopicMessagesPreview).not.toBeNull()) ||
    expect(errors.fetchTopicMessagesPreview).toBeNull();
    (fetchTopicMetrics && expect(errors.fetchTopicMetrics).not.toBeNull()) ||
    expect(errors.fetchTopicMetrics).toBeNull();
    (fetchSubscriptions && expect(errors.fetchSubscriptions).not.toBeNull()) ||
    expect(errors.fetchSubscriptions).toBeNull();
}

function expectNoErrors(errors: UseTopicErrors) {
    expectErrors(errors, {});
}
