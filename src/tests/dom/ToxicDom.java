/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.testing.security.firingrange.tests.dom;

import com.google.common.base.CharMatcher;
import com.google.testing.security.firingrange.utils.Responses;
import com.google.testing.security.firingrange.utils.Templates;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet to provide toxic DOM based XSSes. It is mapped to the whole /dom/toxicdom/
 * directory and maps paths as follows:
 * <ul>
 *     <li> Source object
 *     <li> Source property
 *     <li> Sink
 * </ul>
 *
 * Thus, {@code /dom/toxicdom/document/cookie/eval} will map to the document.cookie source,
 * and the eval sink.
 */
public class ToxicDom extends HttpServlet {

  private static final String TOXIC_TEMPLATE = "toxicdom.tmpl";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String pathInfo = request.getPathInfo().substring(1);
    
    if (CharMatcher.is('/').countIn(pathInfo) == 1) {
      Responses.sendXssed(response, generateAccesslessTemplate(
          pathInfo.split("/")[0], pathInfo.split("/")[1]));
    } else if (CharMatcher.is('/').countIn(pathInfo) != 2) {
      String errorMsg = String.format("Missing sink, not enough sinks or too many sinks :(."
          + "Got %d, expected 2", CharMatcher.is('/').countIn(pathInfo));
      Responses.sendError(response, errorMsg, 400);
    } else {
      try {
        Responses.sendXssed(response, generateTemplate(
            pathInfo.split("/")[0], pathInfo.split("/")[1], pathInfo.split("/")[2]));
      } catch (IllegalArgumentException e) {
        Responses.sendError(response, e.getMessage(), 400);
      }
    }
  }

  private String generateAccesslessTemplate(String source, String sink) {
    String sourcePath = "sources/" + source + "/" + sink + ".tmpl";
    String payload = Templates.getTemplate(sourcePath, ToxicDom.class);
    
    String toxicTemplate = Templates.getTemplate(TOXIC_TEMPLATE, ToxicDom.class);
    return Templates.replacePayload(toxicTemplate, payload);
  }

  private String generateTemplate(String source, String accessType, String sink) {
    String sourcePath = "sources/" + source + "/" + accessType + ".tmpl";
    String sourceTemplate = Templates.getTemplate(sourcePath, ToxicDom.class);
    String sinkPath = "sinks/" + sink + ".tmpl";
    String sinkTemplate = Templates.getTemplate(sinkPath, ToxicDom.class);

    String toxicTemplate = Templates.getTemplate(TOXIC_TEMPLATE, ToxicDom.class);
    return Templates.replacePayload(toxicTemplate, sourceTemplate + sinkTemplate);
  }

}
