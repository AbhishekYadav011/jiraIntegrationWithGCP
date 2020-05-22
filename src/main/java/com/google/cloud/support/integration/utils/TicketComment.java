/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.support.integration.utils;

import java.io.IOException;
import java.util.List;

import com.google.cloudsupport.v1alpha2.model.Comment;

public class TicketComment {
  private TicketComment() {}
  
  public static Comment addCommentToTicket(String caseName, String commentString) 
    throws IOException {
      Comment comment = new Comment();
      comment.setText(commentString);

      return SupportAPIClient.getSupportService().supportAccounts().cases().comments().
        create(caseName, comment).execute();
  }

  public static List<Comment> getTicketComments(String caseName) 
    throws IOException {
      return SupportAPIClient.getSupportService().supportAccounts().cases().comments().
        list(caseName).execute().getComments();
  }

}
