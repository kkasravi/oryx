/*
 * Copyright (c) 2014, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */

package com.cloudera.oryx.ml.serving.als;

import com.cloudera.oryx.common.math.VectorMath;

final class DotsFunction implements DoubleFunction<float[]> {

  private final double[][] userFeaturesVectors;

  DotsFunction(float[] userVector) {
    this(VectorMath.toDoubles(userVector));
  }

  DotsFunction(double[] userVector) {
    this.userFeaturesVectors = new double[][] {userVector};
  }

  DotsFunction(double[][] userFeaturesVectors) {
    this.userFeaturesVectors = userFeaturesVectors;
  }

  @Override
  public double apply(float[] itemVector) {
    double total = 0.0;
    for (double[] userFeatureVector : userFeaturesVectors) {
      total += VectorMath.dot(userFeatureVector, itemVector);
    }
    return total / userFeaturesVectors.length;
  }

}
