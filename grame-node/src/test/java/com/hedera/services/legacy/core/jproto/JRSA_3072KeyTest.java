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

public class JRSA_3072KeyTest {
  @Test
  public void emptyJRSA_3072KeyTest() {
    JRSA_3072Key key1 = new JRSA_3072Key(null);
    assertTrue(key1.isEmpty());
    assertFalse(key1.isValid());

    JRSA_3072Key key2 = new JRSA_3072Key(new byte[0]);
    assertTrue(key2.isEmpty());
    assertFalse(key2.isValid());
  }

  @Test
  public void nonEmptyJRSA_3072KeyTest() {
    JRSA_3072Key key = new JRSA_3072Key(new byte[1]);
    assertFalse(key.isEmpty());
    assertTrue(key.isValid());
  }
}
