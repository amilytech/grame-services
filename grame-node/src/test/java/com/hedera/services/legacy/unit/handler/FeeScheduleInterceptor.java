package com.grame.services.legacy.unit.handler;

/*-
 * ‌
 * grame Services Node
 * ​
 * Copyright (C) 2018 - 2021 grame grame, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.grame.services.fees.FeeCalculator;
import com.gramegrame.api.proto.java.FileID;
import com.grame.services.legacy.unit.FCStorageWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FeeScheduleInterceptor implements GenericInterceptor {
  private static final Logger log = LogManager.getLogger(FeeScheduleInterceptor.class);

  private final FeeCalculator fees;

  public FeeScheduleInterceptor(FeeCalculator fees) {
    this.fees = fees;
  }

  @Override
  public void update(FCStorageWrapper fcfs, FileID fid) {
    if (fid.getFileNum() == 111) {
      try {
        fees.init();
      } catch (Exception e) {
        log.warn("Could not reload fee schedule!", e);
      }
    }
  }
}
