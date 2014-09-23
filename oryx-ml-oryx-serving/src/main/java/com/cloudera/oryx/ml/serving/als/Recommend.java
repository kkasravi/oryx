/*
 * Copyright (c) 2014, Cloudera and Intel, Inc. All Rights Reserved.
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

import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.carrotsearch.hppc.ObjectOpenHashSet;
import com.carrotsearch.hppc.ObjectSet;
import com.google.common.base.Predicate;

import com.cloudera.oryx.common.collection.Pair;
import com.cloudera.oryx.ml.serving.CSVMessageBodyWriter;
import com.cloudera.oryx.ml.serving.IDValue;
import com.cloudera.oryx.ml.serving.OryxServingException;
import com.cloudera.oryx.ml.serving.als.model.ALSServingModel;

/**
 * <p>Responds to a GET request to
 * {@code /recommend/[userID](?howMany=n)(&offset=o)(&considerKnownItems=c)}.</p>
 *
 * <p>Results are recommended items for the user, along with a score.
 * Outputs contain item and score pairs, where the score is an opaque
 * value where higher values mean a better recommendation.</p>
 *
 * <p>{@code offset} is an offset into the entire list of results; {@code howMany} is the desired
 * number of results to return from there. For example, {@code offset=30} and {@code howMany=5}
 * will cause the implementation to retrieve 35 results internally and output the last 5.
 * If {@code howMany} is not specified, defaults to 10. {@code offset} defaults to 0.</p>
 *
 * <p>{@code considerKnownItems} causes items that the user has interacted with to be
 * eligible to be returned as recommendations. It defaults to {@code false}, meaning that these
 * previously interacted-with items are not returned in recommendations.</p>
 *
 * <p>If the user is not known to the model, a {@link Response.Status#NOT_FOUND}
 * response is generated.</p>
 *
 * <p>Default output is CSV format, containing {@code id,value} per line.
 * JSON format can also be selected by an appropriate {@code Accept} header. It returns
 * an array of recommendations, each of which has an "id" and "value" entry, like
 * [{"id":"I2","value":0.141348009071816},...]</p>
 */
@Path("/recommend")
public final class Recommend extends AbstractALSResource {

  @GET
  @Path("{userID}")
  @Produces({CSVMessageBodyWriter.TEXT_CSV, MediaType.APPLICATION_JSON})
  public List<IDValue> get(
      @PathParam("userID") String userID,
      @DefaultValue("10") @QueryParam("howMany") int howMany,
      @DefaultValue("0") @QueryParam("offset") int offset,
      @DefaultValue("false") @QueryParam("considerKnownItems") boolean considerKnownItems,
      @QueryParam("rescorerParams") List<String> rescorerParams) throws OryxServingException {

    check(howMany > 0, "howMany must be positive");
    check(offset >= 0, "offset must be nonnegative");

    ALSServingModel model = getALSServingModel();
    float[] userVector = model.getUserVector(userID);
    checkExists(userVector != null, userID);

    Predicate<String> allowedFn = null;
    if (!considerKnownItems) {
      ObjectSet<String> knownItems = model.getKnownItems(userID);
      if (knownItems != null && !knownItems.isEmpty()) {
        synchronized (knownItems) {
          allowedFn = new NotKnownPredicate(new ObjectOpenHashSet<>(knownItems));
        }
      }
    }

    List<Pair<String,Double>> topIDDots = model.topN(
        new DotsFunction(userVector),
        howMany + offset,
        allowedFn);
    return toIDValueResponse(topIDDots, howMany, offset);
  }

}
