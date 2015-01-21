/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sqoop.connector.idf;

import org.apache.sqoop.classification.InterfaceAudience;
import org.apache.sqoop.classification.InterfaceStability;
import org.apache.sqoop.common.ErrorCode;

@InterfaceAudience.Public
@InterfaceStability.Unstable
public enum JSONIntermediateDataFormatError implements ErrorCode {
  /** An unknown error has occurred. */
  JSON_INTERMEDIATE_DATA_FORMAT_0000("An unknown error has occurred."),

  JSON_INTERMEDIATE_DATA_FORMAT_0001("JSON array parse error."),

  JSON_INTERMEDIATE_DATA_FORMAT_0002("JSON object parse error."),

  ;

  private final String message;

  private JSONIntermediateDataFormatError(String message) {
    this.message = message;
  }

  public String getCode() {
    return name();
  }

  public String getMessage() {
    return message;
  }
}
