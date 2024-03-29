/*
 * Copyright 2016-2021 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.values;

/**
 * User: peter.lawrey Date: 08/10/13 Time: 09:09
 */
public interface GetUsingStringInterface {

    void setAnotherStringField(@MaxUtf8Length(64) String s);

    String getSomeStringField();

    void setSomeStringField(@MaxUtf8Length(64) @NotNull String s);

    void getUsingSomeStringField(StringBuilder builder);

    StringBuilder getUsingAnotherStringField(StringBuilder builder);
}

