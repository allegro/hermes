# Faster manual cleanup of Kafka topic leftovers

## Summary

Deleted Hermes topics can remain on one or more Kafka clusters and appear on <https://hermes.allegrogroup.com/ui/consistency>. This is caused by topic deletion spanning Hermes metadata and Kafka without a transaction or automatic retry.

The leftovers are not currently harmful enough to justify redesigning topic deletion. The accepted operational approach is to review and remove them manually, approximately once a month.

The current UI makes that unnecessarily slow because every topic has to be removed separately. The proposed change is therefore limited to `hermes-console`: add multi-selection and batch removal while continuing to use the existing single-topic management endpoint.

## What is happening

The consistency page reports physical Kafka topics that do not have matching Hermes metadata.

Normal topic deletion follows this order:

1. Remove related subscriptions and schema.
2. Remove Hermes topic metadata from Zookeeper.
3. Remove the physical topic or topics from each Kafka cluster.

Steps 2 and 3 are not transactional. If Kafka deletion fails or the management process stops after metadata removal, the Kafka topic remains while Hermes no longer knows about it. The consistency checker then reports it as an inconsistent topic.

Because `MultiDCAwareService.listTopicFromAllDC()` returns a union of topic names from all Kafka datacenters, a topic left in only one cluster is enough to appear in the UI.

Relevant backend code:

- `hermes-management/src/main/java/pl/allegro/tech/hermes/management/domain/topic/TopicService.java`, method `removeTopic`
- `hermes-management/src/main/java/pl/allegro/tech/hermes/management/domain/consistency/KafkaHermesConsistencyService.java`
- `hermes-management/src/main/java/pl/allegro/tech/hermes/management/infrastructure/kafka/MultiDCAwareService.java`
- `hermes-management/src/main/java/pl/allegro/tech/hermes/management/infrastructure/kafka/service/KafkaBrokerTopicManagement.java`

## Investigation

### Existing UI flow

`ConsistencyView.vue` displays `InconsistentTopicsListing.vue`. Each row has a separate **Remove** button. Clicking it opens a confirmation dialog and, after confirmation, calls:

```text
DELETE /consistency/inconsistencies/topics?topicName=<Kafka topic name>
```

After a successful request, the whole route is reloaded. Cleaning many topics therefore requires one confirmation and one page reload per topic.

Relevant console code:

- `hermes-console/src/views/admin/consistency/ConsistencyView.vue`
- `hermes-console/src/views/admin/consistency/inconsistent-topics-listing/InconsistentTopicsListing.vue`
- `hermes-console/src/composables/inconsistent-topics/use-inconsistent-topics/useInconsistentTopics.ts`
- `hermes-console/src/api/hermes-client/index.ts`, function `removeInconsistentTopic`

### Existing backend behavior

The backend cleanup endpoint accepts one raw Kafka topic name. Adding a batch endpoint would be more efficient, but it is not necessary for the expected monthly administrative cleanup. The console can call the existing endpoint once for every selected topic.

The endpoint starts deletion on all configured Kafka clusters. `BrokersClusterService.removeTopicByName()` does not wait for Kafka's asynchronous result, so HTTP success means the delete request was submitted, not that absence from Kafka was verified. This behavior already exists for single deletion and is outside the scope of this UI-only change.

## Chosen solution

Add checkbox-based selection to the inconsistent topics table and a batch **Remove selected** action.

The intended operator flow is:

1. Open the inconsistent topics section.
2. Optionally filter topics using the existing search field.
3. Choose **Select all** to select all currently visible, filtered topics.
4. Deselect internal or otherwise protected topics that should remain.
5. Click **Remove selected**.
6. Review a confirmation showing the number of selected topics.
7. Confirm once and let the UI send one existing DELETE request per selected topic.
8. See which removals succeeded or failed without reloading the page after every request.

Selection is intentionally manual. This avoids embedding assumptions about internal-topic naming conventions in the console. Operators can use the existing filter plus **Select all**, then opt out individual topics before deletion.

## Detailed UI behavior

### Selection

- Add a checkbox column before the row index.
- Add a header checkbox that selects or deselects all topics visible under the current filter.
- Keep selections when the search filter changes so operators can build a selection across multiple searches.
- The header checkbox is checked when every visible topic is selected and indeterminate when only some visible topics are selected.
- Every row can be selected or deselected independently, allowing internal topics to be excluded.
- Clear selections for topics that disappear from the loaded topic list after successful removal.
- Show the number of selected topics near the batch action.

"Select all" means all currently filtered topics, not every hidden topic in the complete list. This makes the existing search field useful as a safety boundary and avoids accidentally selecting topics that the operator cannot see.

### Batch action

- Add a **Remove selected** button above the table.
- Disable it when no topic is selected or while a batch is running.
- Keep the existing per-row **Remove** action for occasional single-topic cleanup.
- Open one confirmation dialog for the batch.
- Include the selected count in the confirmation text.
- For a small selection, optionally list topic names in the dialog; for a large selection, show the count and a short preview instead of an unbounded list.

### Request execution

- Reuse `removeInconsistentTopic(topic)` and the existing endpoint; do not add a batch backend endpoint.
- Process requests with limited concurrency rather than firing an unbounded `Promise.all`. A small fixed number, such as five concurrent requests, avoids overwhelming management or the browser while keeping cleanup fast.
- Continue processing other selected topics when one request fails.
- Track success and failure per topic.
- Remove successful topics from local UI state instead of calling `router.go(0)` after each deletion.
- Keep failed topics visible and selected so the operator can retry them.
- Refresh the inconsistent-topic list once after the batch finishes to reflect the server's latest view.

### Feedback

- While deletion is running, show progress such as `Removing 12 of 80 topics`.
- After completion, show one summary notification instead of one notification per topic, for example `Removed 77 topics; 3 failed`.
- If failures occurred, show the failed topic names in the UI and provide a **Retry failed** action or leave them selected for another **Remove selected** click.
- Do not report the entire batch as failed because one request failed.

## Implementation plan

### 1. Add selection to the listing

Update `InconsistentTopicsListing.vue` to:

- accept the selected topic names through `v-model` or equivalent `selectedTopics`/`update:selectedTopics` props and events;
- render row and header checkboxes;
- calculate select-all and indeterminate state from `filteredTopics`;
- select/deselect only `filteredTopics` from the header checkbox;
- emit the existing single-row `remove` event unchanged.

Keep ownership of the selected set in `ConsistencyView.vue`, because the view owns deletion and confirmation behavior.

### 2. Support local refresh after deletion

Extend `useInconsistentTopics()` with one of these minimal interfaces:

- expose `fetchInconsistentTopics()` and call it after the batch; and
- remove successfully submitted names from `topicNames` immediately so progress is visible without a route reload.

Prefer exposing a batch orchestration function from the composable only if it materially simplifies notification and state handling. The API client should remain single-topic because the server API is single-topic.

### 3. Add batch orchestration to the view

Update `ConsistencyView.vue` to:

- store selected topic names;
- open a batch confirmation dialog;
- run deletion with bounded concurrency;
- collect successful and failed names;
- update progress state;
- remove successful names from selection;
- keep failures selected;
- refresh once at the end;
- retain the current single-topic flow without a full route reload.

### 4. Add translations

Add English translations for:

- select all topics;
- selected topic count;
- remove selected;
- batch confirmation title and text;
- progress;
- complete success, partial success, and complete failure;
- retry failed, if implemented as a separate action.

### 5. Add tests

Update `InconsistentTopicsListing.spec.ts` to cover:

- selecting one row;
- selecting all visible topics;
- select-all with an active filter does not select hidden topics;
- deselecting an internal topic after select-all;
- checked and indeterminate header states;
- preserving selections when the filter changes.

Update `ConsistencyView.spec.ts` to cover:

- batch button disabled with no selection;
- one confirmation for multiple selected topics;
- each selected topic is passed to the existing removal function;
- successful removals are cleared from the list and selection;
- one failed request does not stop remaining removals;
- failed topics stay selected and a partial-failure summary is shown;
- controls are disabled and progress is displayed while deletion runs.

Update composable tests if local removal, refresh, or batch result handling is added there.

## Out of scope

- Changing topic deletion ordering.
- Adding transactions or rollback between Zookeeper and Kafka.
- Persisting deletion intents or adding automatic retries.
- Adding a backend batch endpoint.
- Changing the existing Kafka AdminClient completion behavior.
- Automatically identifying internal topics by a hard-coded prefix or pattern.
- Changing the consistency API to report per-datacenter details.

These can be reconsidered if leftovers become frequent, harmful, or too numerous for monthly manual cleanup.

## Acceptance criteria

- An operator can select all topics currently visible under the active filter with one action.
- An operator can deselect any topic, including internal topics, before deletion.
- Hidden topics are not selected by **Select all**.
- One confirmation starts deletion of every selected topic through the existing endpoint.
- A failure for one topic does not stop deletion of the remaining selected topics.
- Successful topics disappear from the list without a page reload per topic.
- Failed topics remain visible and easy to retry.
- The UI displays batch progress and a final success/partial-failure summary.
- Existing single-topic removal remains available.
- No backend changes are required.

## Risks and safeguards

- **Accidental broad deletion:** limit select-all to visible filtered rows, show a selected count, and require one explicit confirmation.
- **Internal topics selected accidentally:** allow row-level opt-out and do not automatically classify names without an agreed naming rule.
- **Too many simultaneous requests:** use bounded concurrency.
- **Partial completion:** collect per-topic results, continue the batch, and retain failures for retry.
- **Kafka deletion is asynchronous:** refresh once after the batch. A topic may remain visible if Kafka has not completed deletion yet and can be checked again during the next cleanup session.
