/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.cdi;

import org.apache.camel.component.properties.PropertiesComponent;

import javax.enterprise.inject.Vetoed;
import java.util.Properties;

@Vetoed
public final class CdiPropertiesComponent extends PropertiesComponent {

    public CdiPropertiesComponent(Properties properties) {
        setPropertiesParser(new CdiPropertiesParser(this, properties));
    }

    public CdiPropertiesComponent(String location) {
        setLocation(location);
    }

    public CdiPropertiesComponent(String...locations) {
        setLocations(locations);
    }

    @Override
    // Overridden to fix exception messages thrown when an undefined property is looked up as the default implementation solely relies on the nullity of the locations member
    public boolean isDefaultCreated() {
        return false;
    }
}
