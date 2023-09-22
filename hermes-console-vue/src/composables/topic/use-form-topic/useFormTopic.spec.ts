import { afterEach, expect } from 'vitest';
import { createTestingPiniaWithState } from '@/dummy/store';
import {
  dummyAckModes,
  dummyContentTypes,
  dummyOwnerSources,
  dummyRetentionUnits,
  dummyTopicForm,
  dummyTopicFormValidator,
} from '@/dummy/topic-form';
import { fetchOwnerSourcesHandler } from '@/mocks/handlers';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useFormTopic } from '@/composables/topic/use-form-topic/useFormTopic';
import { waitFor } from '@testing-library/vue';

describe('useFormTopic', () => {
  const server = setupServer(fetchOwnerSourcesHandler(dummyOwnerSources));

  beforeEach(() => {
    setActivePinia(createTestingPiniaWithState());
  });

  afterEach(() => {
    server.resetHandlers();
  });

  it('should return form and validators', async () => {
    //given
    server.listen();

    // when
    const { form, validators } = useFormTopic();

    // then
    expect(JSON.stringify(form.value)).toMatchObject(
      JSON.stringify(dummyTopicForm),
    );
    expect(JSON.stringify(validators)).toMatchObject(
      JSON.stringify(dummyTopicFormValidator),
    );
  });

  it('should return dataSources', async () => {
    //given
    server.listen();

    // when
    const { dataSources } = useFormTopic();

    // then
    await waitFor(() => {
      expect(JSON.stringify(dataSources.contentTypes.value)).toMatchObject(
        JSON.stringify(dummyContentTypes),
      );
      expect(JSON.stringify(dataSources.ackModes)).toMatchObject(
        JSON.stringify(dummyAckModes),
      );
      expect(JSON.stringify(dataSources.retentionUnits)).toMatchObject(
        JSON.stringify(dummyRetentionUnits),
      );
      expect(JSON.stringify(dataSources.ownerSources.value)).toMatchObject(
        JSON.stringify(
          dummyOwnerSources
            .filter((source) => !source.deprecated)
            .map((source) => {
              return { title: source.name, value: source };
            }),
        ),
      );
    });
  });
});
