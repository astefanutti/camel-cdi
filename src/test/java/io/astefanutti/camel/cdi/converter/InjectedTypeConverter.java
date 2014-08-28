/**
 * Copyright (C) 2014 Antonin Stefanutti (antonin.stefanutti@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.astefanutti.camel.cdi.converter;

import io.astefanutti.camel.cdi.pojo.TypeConverterInput;
import io.astefanutti.camel.cdi.pojo.TypeConverterOutput;
import org.apache.camel.CamelContext;
import org.apache.camel.Converter;

import javax.inject.Inject;

@Converter
public final class InjectedTypeConverter {

    private final CamelContext context;
    
    @Inject
    InjectedTypeConverter(CamelContext context) {
        this.context = context;
    }

    @Converter
    public TypeConverterOutput convert(TypeConverterInput input) throws Exception {
        TypeConverterOutput output = new TypeConverterOutput();
        output.setProperty(context.resolvePropertyPlaceholders(input.getProperty()));
        return output;
    }
}
