package com.grame.services.legacy.core.jproto;

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JECDSA_384KeyTest {
  @Test
  public void emptyJECDSA_384KeyTest() {
    JECDSA_384Key key1 = new JECDSA_384Key(null);
    assertTrue(key1.isEmpty());
    assertFalse(key1.isValid());

    JECDSA_384Key key2 = new JECDSA_384Key(new byte[0]);
    assertTrue(key2.isEmpty());
    assertFalse(key2.isValid());
  }

  @Test
  public void nonEmptyJECDSA_384KeyTest() {
    JECDSA_384Key key = new JECDSA_384Key(new byte[1]);
    assertFalse(key.isEmpty());
    assertTrue(key.isValid());
  }
}
