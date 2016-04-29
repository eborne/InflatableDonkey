/* 
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.horrorho.inflatabledonkey.args;

import java.util.LinkedHashMap;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.apache.commons.cli.Option;

/**
 * Options factory.
 *
 * @author Ahseya
 */
@Immutable
public final class OptionsFactory {

    private OptionsFactory() {
    }

    public static Map<Option, Property> options() {
        LinkedHashMap<Option, Property> options = new LinkedHashMap<>();

        options.put(Option.builder("d").longOpt("device")
                .desc("Device, default: 0 = first device.")
                .argName("int")
                .hasArg()
                .build(),
                Property.SELECT_DEVICE_INDEX);

        options.put(
                Option.builder().longOpt("serial")
                    .desc("Device serial number, case insensitive.")
                    .argName("string")
                    .hasArg()
                    .build(),
                Property.SELECT_DEVICE_SERIALNUMBER
            );

        options.put(Option.builder("s").longOpt("snapshot")
                .desc("Snapshot, default: 0 = first snapshot.")
                .argName("int")
                .hasArg()
                .build(),
                Property.SELECT_SNAPSHOT_INDEX);

        options.put(Option.builder().longOpt("extension")
                .desc("File extension filter, case insensitive.")
                .argName("string")
                .hasArg()
                .build(),
                Property.FILTER_EXTENSION);

        options.put(Option.builder().longOpt("domain")
                .desc("Domain filter, case insensitive. Separate values with commas.")
                .argName("string")
                .hasArg()
                .build(),
                Property.FILTER_DOMAIN);

        options.put(Option.builder("o").longOpt("folder")
                .desc("Output folder. Defaults to device serial number.")
                .argName("string")
                .hasArg()
                .build(),
                Property.OUTPUT_FOLDER);

        options.put(new Option(null, "snapshots", false, "List device/ snapshot information and exit."),
                Property.PRINT_SNAPSHOTS);

        options.put(new Option(null, "domains", false, "List domains/ file count for the selected snapshot and exit."),
                Property.PRINT_DOMAIN_LIST);

        options.put(
                new Option(null, "token", false, "Display dsPrsID:mmeAuthToken and exit."),
                Property.ARGS_TOKEN);

        options.put(
                new Option(null, "help", false, "Display this help and exit."),
                Property.ARGS_HELP);

        return options;
    }
}
