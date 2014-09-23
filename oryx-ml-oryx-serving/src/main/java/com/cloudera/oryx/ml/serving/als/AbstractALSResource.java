/*
 * Copyright (c) 2014, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */

package com.cloudera.oryx.ml.serving.als;

import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import com.cloudera.oryx.common.collection.Pair;
import com.cloudera.oryx.lambda.QueueProducer;
import com.cloudera.oryx.lambda.serving.ServingModelManager;
import com.cloudera.oryx.ml.serving.IDValue;
import com.cloudera.oryx.ml.serving.OryxServingException;
import com.cloudera.oryx.ml.serving.als.model.ALSServingModel;

public abstract class AbstractALSResource {

  public static final String MODEL_MANAGER_KEY =
      "com.cloudera.oryx.lambda.serving.ModelManagerListener.ModelManager";
  public static final String INPUT_PRODUCER_KEY =
      "com.cloudera.oryx.lambda.serving.ModelManagerListener.InputProducer";

  @Context
  private ServletContext servletContext;
  private ALSServingModel alsServingModel;
  private QueueProducer<String,String> inputProducer;

  @SuppressWarnings("unchecked")
  @PostConstruct
  public void init() {
    ServingModelManager<?> servingModelManager = (ServingModelManager<?>)
        servletContext.getAttribute(MODEL_MANAGER_KEY);
    alsServingModel = (ALSServingModel) servingModelManager.getModel();
    inputProducer = (QueueProducer<String,String>) servletContext.getAttribute(INPUT_PRODUCER_KEY);
  }

  protected final ServletContext getServletContext() {
    return servletContext;
  }

  protected final ALSServingModel getALSServingModel() {
    return alsServingModel;
  }

  protected final QueueProducer<?,String> getInputProducer() {
    return inputProducer;
  }

  protected static void check(boolean condition,
                              Response.Status status,
                              String errorMessage) throws OryxServingException {
    if (!condition) {
      throw new OryxServingException(status, errorMessage);
    }
  }

  protected static void check(boolean condition,
                              String errorMessage) throws OryxServingException {
    check(condition, Response.Status.BAD_REQUEST, errorMessage);
  }

  protected static void checkExists(boolean condition,
                                    String entity) throws OryxServingException {
    check(condition, Response.Status.NOT_FOUND, entity);
  }

  protected static <T> List<T> selectedSublist(List<T> values, int howMany, int offset) {
    if (values.size() < offset) {
      return Collections.emptyList();
    }
    return values.subList(offset, Math.min(offset + howMany, values.size()));
  }

  protected static List<IDValue> toIDValueResponse(List<Pair<String,Double>> pairs,
                                                   int howMany,
                                                   int offset) {
    List<Pair<String,Double>> sublist = selectedSublist(pairs, howMany, offset);
    return Lists.transform(sublist,
        new Function<Pair<String,Double>,IDValue>() {
          @Override
          public IDValue apply(Pair<String,Double> idDot) {
            return new IDValue(idDot.getFirst(), idDot.getSecond());
          }
        });
  }

}
