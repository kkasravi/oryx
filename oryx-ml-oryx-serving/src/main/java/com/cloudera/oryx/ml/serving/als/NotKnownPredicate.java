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

import com.carrotsearch.hppc.ObjectSet;
import com.google.common.base.Predicate;

final class NotKnownPredicate implements Predicate<String> {

  private final ObjectSet<String> knownItemsForUser;

  /**
   * @param knownItemsForUser items which are already known to the user. The object should
   *  be safely accessible without synchronization.
   */
  NotKnownPredicate(ObjectSet<String> knownItemsForUser) {
    this.knownItemsForUser = knownItemsForUser;
  }

  @Override
  public boolean apply(String input) {
    return !knownItemsForUser.contains(input);
  }

}
