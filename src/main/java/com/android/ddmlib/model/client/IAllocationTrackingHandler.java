/*
 * Copyright (C) 2019 Anton Malinskiy
 *
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
 */

package com.android.ddmlib.model.client;

import com.android.ddmlib.Client;
import com.android.ddmlib.utils.AllocationsParser;
import org.jetbrains.annotations.NotNull;

/*
 * Handlers able to act on allocation tracking info
 */
public interface IAllocationTrackingHandler {
  /**
   * Called when an allocation tracking was successful.
   * @param data the data containing the encoded allocations.
   *             See {@link AllocationsParser#parse(java.nio.ByteBuffer)} for parsing this data.
   * @param client the client for which allocations were tracked.
   */
  void onSuccess(@NotNull byte[] data, @NotNull Client client);
}
