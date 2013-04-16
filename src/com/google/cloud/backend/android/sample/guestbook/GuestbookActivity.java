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
package com.google.cloud.backend.android.sample.guestbook;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.cloud.backend.android.CloudBackendActivity;
import com.google.cloud.backend.android.CloudCallbackHandler;
import com.google.cloud.backend.android.CloudEntity;
import com.google.cloud.backend.android.CloudQuery.Order;
import com.google.cloud.backend.android.CloudQuery.Scope;
import com.google.cloud.backend.android.R;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Sample Guestbook app with Mobile Backend Starter.
 *
 */
public class GuestbookActivity extends CloudBackendActivity {

  // data formatter for formatting createdAt property
  private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss ", Locale.US);

  private static final String BROADCAST_PROP_DURATION = "duration";

  private static final String BROADCAST_PROP_MESSAGE = "message";

  // UI components
  private TextView tvPosts;
  private EditText etMessage;
  private Button btSend;

  // a list of posts on the UI
  List<CloudEntity> posts = new LinkedList<CloudEntity>();

  // initialize UI
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    tvPosts = (TextView) findViewById(R.id.tvPosts);
    etMessage = (EditText) findViewById(R.id.etMessage);
    btSend = (Button) findViewById(R.id.btSend);
  }

  @Override
  protected void onPostCreate() {
    super.onPostCreate();
    listAllPosts();
  }

  // execute query "SELECT * FROM Guestbook ORDER BY _createdAt DESC LIMIT 50"
  // this query will be re-executed when matching entity is updated
  private void listAllPosts() {

    // create a response handler that will receive the query result or an error
    CloudCallbackHandler<List<CloudEntity>> handler = new CloudCallbackHandler<List<CloudEntity>>() {
      @Override
      public void onComplete(List<CloudEntity> results) {
        posts = results;
        updateGuestbookUI();
      }

      @Override
      public void onError(IOException exception) {
        handleEndpointException(exception);
      }
    };

    // execute the query with the handler
    getCloudBackend().listByKind("Guestbook", CloudEntity.PROP_CREATED_AT, Order.DESC, 50,
        Scope.FUTURE_AND_PAST, handler);
  }

  private void handleEndpointException(IOException e) {
    Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
    btSend.setEnabled(true);
  }

  // convert posts into string and update UI
  private void updateGuestbookUI() {
    final StringBuilder sb = new StringBuilder();
    for (CloudEntity post : posts) {
      sb.append(sdf.format(post.getCreatedAt()) + getCreatorName(post) + ": " + post.get("message")
          + "\n");
    }
    tvPosts.setText(sb.toString());
  }

  // removing the domain name part from email address
  private String getCreatorName(CloudEntity b) {
    if (b.getCreatedBy() != null) {
      return " " + b.getCreatedBy().replaceFirst("@.*", "");
    } else {
      return "<anonymous>";
    }
  }

  // post a new message to server
  public void onSendButtonPressed(View view) {

    // create a CloudEntity with the new post
    CloudEntity newPost = new CloudEntity("Guestbook");
    newPost.put("message", etMessage.getText().toString());

    // create a response handler that will receive the result or an error
    CloudCallbackHandler<CloudEntity> handler = new CloudCallbackHandler<CloudEntity>() {
      @Override
      public void onComplete(final CloudEntity result) {
        posts.add(0, result);
        updateGuestbookUI();
        etMessage.setText("");
        btSend.setEnabled(true);
      }

      @Override
      public void onError(final IOException exception) {
        handleEndpointException(exception);
      }
    };

    // execute the insertion with the handler
    getCloudBackend().insert(newPost, handler);
    btSend.setEnabled(false);
  }

  // handles broadcast message and show a toast
  @Override
  public void onBroadcastMessageReceived(List<CloudEntity> l) {
    for (CloudEntity e : l) {
      String message = (String) e.get(BROADCAST_PROP_MESSAGE);
      int duration = Integer.parseInt((String) e.get(BROADCAST_PROP_DURATION));
      Toast.makeText(this, message, duration).show();
    }
  }
}
