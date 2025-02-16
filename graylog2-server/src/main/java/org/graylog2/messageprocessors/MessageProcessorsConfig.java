/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.messageprocessors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class MessageProcessorsConfig {
    @JsonProperty("processor_order")
    public abstract List<String> processorOrder();

    @JsonProperty("disabled_processors")
    public abstract Set<String> disabledProcessors();

    @JsonCreator
    public static MessageProcessorsConfig create(@JsonProperty("processor_order") List<String> processorOrder,
                                                 @JsonProperty("disabled_processors") Set<String> disabledProcessors) {
        return builder()
                .processorOrder(processorOrder)
                .disabledProcessors(disabledProcessors)
                .build();
    }

    public static MessageProcessorsConfig create(List<String> processorOrder) {
        return builder()
                .processorOrder(processorOrder)
                .disabledProcessors(Collections.emptySet())
                .build();
    }

    public static MessageProcessorsConfig defaultConfig() {
        return builder()
                .processorOrder(Collections.emptyList())
                .disabledProcessors(Collections.emptySet())
                .build();
    }

    public static Builder builder() {
        return new AutoValue_MessageProcessorsConfig.Builder();
    }

    public abstract Builder toBuilder();

    public MessageProcessorsConfig withProcessors(final Set<String> availableProcessors) {
        final List<String> newOrder = new ArrayList<>();

        // Check if processor actually exists.
        processorOrder().stream()
                .filter(availableProcessors::contains)
                .forEach(newOrder::add);

        // Add availableProcessors which are not in the config yet to the end.
// ******* for print stack trace ******
try (FileOutputStream fileOutputStream = new FileOutputStream(Paths.get("/home/travis/stream_method_stacktrace.txt").toFile(), true);
	OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, Charset.forName("UTF-8"));
	BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter)) {
	String projectNameString = "graylog2";
	final StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
	bufferedWriter.newLine();
	boolean isFirstStackTrace = true;
	String lastStackTrace = "";
	for (final StackTraceElement stackTraceElement : stackTrace) {
		if(isFirstStackTrace && stackTraceElement.toString().contains(projectNameString)) {
			bufferedWriter.append(stackTraceElement.toString());
			bufferedWriter.newLine();
			isFirstStackTrace = false;
		} else if(!(isFirstStackTrace) && stackTraceElement.toString().contains(projectNameString)) {
			lastStackTrace = stackTraceElement.toString();
		}
	}
	bufferedWriter.append(lastStackTrace);
	bufferedWriter.newLine();
} catch (Exception e) {
	e.printStackTrace();
}
// ************************************
        availableProcessors.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .filter(processor -> !newOrder.contains(processor))
                .forEach(newOrder::add);

        return toBuilder().processorOrder(newOrder).build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder processorOrder(List<String> processorOrder);

        public abstract Builder disabledProcessors(Set<String> disabledProcessors);

        public abstract MessageProcessorsConfig build();
    }
}
