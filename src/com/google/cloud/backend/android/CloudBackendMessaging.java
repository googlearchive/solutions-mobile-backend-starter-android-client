/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.cloud.backend.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.api.client.util.DateTime;
import com.google.cloud.backend.android.CloudQuery.Order;
import com.google.cloud.backend.android.CloudQuery.Scope;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cloud Backend API class that provides pub/sub messaging feature in addition
 * to the features of {@link CloudBackendAsync}.
 *
 */
public class CloudBackendMessaging extends CloudBackendAsync {

  /**
   * Creates an instance of {@link CloudBackendAsync}. Caller need to pass a
   * {@link Context} such as {@link Activity} that will be used to Google Cloud
   * Messaging.
   *
   * @param application
   *          {@link Context}
   */
  public CloudBackendMessaging(Context context) {
    super(context);
  }

  /**
   * Returns {@link SharedPreferences} that can be used for storing any for
   * Cloud Backend related preferences.
   *
   * @return {@link SharedPreferences}
   */
  public SharedPreferences getSharedPreferences() {
    return application.getSharedPreferences(Consts.PREF_KEY_CLOUD_BACKEND, Context.MODE_PRIVATE);
  }

  /**
   * Kind name of {@link CloudEntity} for Cloud Message.
   */
  public static final String KIND_NAME_CLOUD_MESSAGES = "_CloudMessages";

  /**
   * Property name of _CloudMessages kind that holds topicId.
   */
  public static final String PROP_TOPIC_ID = "topicId";

  /**
   * TopicId for broadcast messages.
   */
  public static final String TOPIC_ID_BROADCAST = "_broadcast";

  // SharedPreference key for timestamp of the last message
  private static final String PREF_KEY_PREFIX_MSG_TIMESTAMP = "PREF_KEY_PREFIX_MSG_TIMESTAMP";

  // subscription for Cloud Message would last for 7 days
  private static final int SUBSCRIPTION_DURATION_FOR_PUSH_MESSAGE = 60 * 60 * 24 * 7;

  // max number of past messages to receive
  private static final int DEFAULT_MAX_MESSAGES_TO_RECEIVE = 100;

  // Map of handlers for Cloud Messages (key = topicId)
  private static final Map<String, CloudCallbackHandler<List<CloudEntity>>> cloudMessageHandlers = new HashMap<String, CloudCallbackHandler<List<CloudEntity>>>();

  /**
   * Subscribes to Cloud Messages for the specified Topic ID. Developer may use
   * any string (that does not include any space chars) as a Topic ID, such as
   * an ID for user, group, hash tag, keyword, or etc.
   *
   * By specifying the optional maxOfflineMessages number, you can receive
   * off-line messages after the last message received. These include the
   * messages sent to the topicId while the app have been off-line, wasn't
   * running, or before the app launched for the first time.
   *
   * @param topicId
   *          Topic ID for the subscription
   * @param cloudMsgHandler
   *          {@link CloudCallbackHandler} that will handle Cloud Message for
   *          the topic. It will receive {@link List} of {@link CloudEntity}s
   *          that contain the messages.
   * @param maxOfflineMessages
   *          (optional) if you want to receive all the off-line messages sent
   *          after the last message received, set max number of the messages to
   *          receive.
   */
  public void subscribeToCloudMessage(String topicId,
      CloudCallbackHandler<List<CloudEntity>> handler, int... maxOfflineMessages) {

    // register the hander for this topic id
    cloudMessageHandlers.put(topicId, handler);

    // create and execute a query for Cloud Message
    int maxOfflineMsgs = maxOfflineMessages.length > 0 ? maxOfflineMessages[0] : 0;
    CloudQuery cq = createQueryForCloudMessage(topicId, maxOfflineMsgs);
    super.list(cq, new CloudMessageHandler());
  }

  private CloudQuery createQueryForCloudMessage(String topicId, int maxOfflineMessages) {

    // whether the query should include past messages since the last message
    boolean includeOfflineMessages = maxOfflineMessages > 0;
    long lastTime = (new Date()).getTime();
    if (includeOfflineMessages) {
      lastTime = getSharedPreferences().getLong(getPrefKeyForTopicId(topicId), lastTime);
    }

    // create query
    CloudQuery cq = new CloudQuery(KIND_NAME_CLOUD_MESSAGES);
    cq.setFilter(F.and(F.eq(PROP_TOPIC_ID, topicId),
        F.gt(CloudEntity.PROP_CREATED_AT, new DateTime(lastTime))));
    cq.setSort(CloudEntity.PROP_CREATED_AT, Order.DESC);
    cq.setSubscriptionDurationSec(SUBSCRIPTION_DURATION_FOR_PUSH_MESSAGE);
    if (includeOfflineMessages) {
      cq.setLimit(maxOfflineMessages);
      cq.setScope(Scope.FUTURE_AND_PAST);
    } else {
      cq.setLimit(DEFAULT_MAX_MESSAGES_TO_RECEIVE);
      cq.setScope(Scope.FUTURE);
    }

    // set topicId as a queryId. Any queries on the same topic will
    // be treated as just one query.
    cq.setQueryId(topicId);
    return cq;
  }

  /**
   * Unsubscribes from Cloud Message for the specified topicId.
   *
   * @param topicId
   */
  public void unsubscribeFromCloudMessage(String topicId) {
    cloudMessageHandlers.remove(topicId);
    CloudBackendAsync.continuousQueries.remove(topicId);
  }

  private String getPrefKeyForTopicId(String topicId) {
    return PREF_KEY_PREFIX_MSG_TIMESTAMP + ":" + topicId;
  }

  // handles Cloud Message
  private class CloudMessageHandler extends CloudCallbackHandler<List<CloudEntity>> {

    @Override
    public void onComplete(List<CloudEntity> messages) {

      // skip if it's empty
      if (messages.isEmpty()) {
        return;
      }

      // get the last message and store the timestamp in the shared pref
      CloudEntity lastMsg = messages.get(0);
      String topicId = (String) lastMsg.get(PROP_TOPIC_ID);
      long lastTime = lastMsg.getCreatedAt().getTime();
      SharedPreferences.Editor e = getSharedPreferences().edit();
      e.putLong(getPrefKeyForTopicId(topicId), lastTime);
      e.commit();

      // find handler for this topic
      CloudCallbackHandler<List<CloudEntity>> handler = cloudMessageHandlers.get(topicId);
      if (handler == null) {
        return;
      }

      // refresh subscriber query to receive new messages from now
      CloudQuery cq = createQueryForCloudMessage(topicId, DEFAULT_MAX_MESSAGES_TO_RECEIVE);
      ContinuousQueryHandler cqh = CloudBackendAsync.continuousQueries.get(topicId);
      CloudBackendAsync.continuousQueries.put(topicId, new ContinuousQueryHandler(cqh.getHandler(),
          cq, getCredential()));

      // sort messages by createdAt ASC
      Collections.reverse(messages);

      // call the handler
      handler.onComplete(messages);
    }
  }

  /**
   * Sends a Cloud Message to the specified topicId.
   *
   * @param message
   *          properties to include in the message
   */
  public void sendCloudMessage(CloudEntity message) {
    super.insert(message, null); // no callback
  }

  /**
   * Sends a Cloud Message to the specified topicId.
   *
   * @param message
   *          properties to include in the message
   * @param handler
   *          {@link CloudCallbackHandler} that has {@link #onComplete(List)}
   *          which will be called after sending specified message to backend,
   *          or {@link #onError(IOException)} which will be called on error.
   */
  public void sendCloudMessage(CloudEntity message, CloudCallbackHandler<CloudEntity> handler) {
    super.insert(message, handler);
  }

  /**
   * Creates a {@link CloudEntity} that represent a Cloud Message. You can use
   * {@link #sendCloudMessage(CloudEntity)} to send out the message.
   *
   * @param topicId
   * @return
   */
  public CloudEntity createCloudMessage(String topicId) {
    CloudEntity ce = new CloudEntity(KIND_NAME_CLOUD_MESSAGES);
    ce.put(PROP_TOPIC_ID, topicId);
    return ce;
  }

  /**
   * Creates a {@link CloudEntity} for message broadcasting. You can use
   * {@link #sendCloudMessage(CloudEntity)} to send out the message.
   *
   * @return {@link CloudEntity}
   */
  public CloudEntity createBroadcastMessage() {
    return createCloudMessage(TOPIC_ID_BROADCAST);
  }

}
