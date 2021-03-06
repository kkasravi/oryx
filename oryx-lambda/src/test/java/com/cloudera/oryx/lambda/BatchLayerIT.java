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

package com.cloudera.oryx.lambda;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.typesafe.config.Config;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.oryx.common.settings.ConfigUtils;

/**
 * Tests {@link BatchLayer}.
 */
public final class BatchLayerIT extends AbstractBatchIT {

  private static final Logger log = LoggerFactory.getLogger(BatchLayerIT.class);

  private static final int DATA_TO_WRITE = 600;
  private static final int WRITE_INTERVAL_MSEC = 100;
  private static final int GEN_INTERVAL_SEC = 15;
  private static final int BLOCK_INTERVAL_SEC = 3;

  @Test
  public void testBatchLayer() throws Exception {
    Path tempDir = getTempDir();
    Path dataDir = tempDir.resolve("data");
    Map<String,String> overlayConfig = new HashMap<>();
    overlayConfig.put("batch.update-class", MockBatchUpdate.class.getName());
    overlayConfig.put("batch.storage.data-dir",
                      "\"" + dataDir.toUri() + "\"");
    overlayConfig.put("batch.storage.model-dir",
                      "\"" + tempDir.resolve("model").toUri() + "\"");
    overlayConfig.put("batch.generation-interval-sec",
                      Integer.toString(GEN_INTERVAL_SEC));
    overlayConfig.put("batch.block-interval-sec",
                      Integer.toString(BLOCK_INTERVAL_SEC));
    overlayConfig.put("batch.storage.partitions", "2");
    Config config = ConfigUtils.overlayOn(overlayConfig, getConfig());

    startMessageQueue();

    List<IntervalData<String,String>> intervalData = MockBatchUpdate.getIntervalDataHolder();

    startServerProduceConsumeQueues(config, DATA_TO_WRITE, WRITE_INTERVAL_MSEC);

    int numIntervals = intervalData.size();
    log.info("{} intervals: {}", numIntervals, intervalData);

    checkOutputData(dataDir, DATA_TO_WRITE);
    checkIntervals(numIntervals, DATA_TO_WRITE, WRITE_INTERVAL_MSEC, GEN_INTERVAL_SEC);

    IntervalData<String,String> last = intervalData.get(0);
    log.info("Interval 0: {}", last);
    for (int i = 1; i < numIntervals; i++) {
      IntervalData<String,String> current = intervalData.get(i);
      log.info("Interval {}: {}", i, current);
      assertTrue(current.getTimestamp() > last.getTimestamp());
      assertTrue(current.getPastData().size() >= last.getPastData().size());
      assertEquals(last.getPastData().size() + last.getCurrentData().size(),
                   current.getPastData().size());
      last = current;
    }
  }

}
