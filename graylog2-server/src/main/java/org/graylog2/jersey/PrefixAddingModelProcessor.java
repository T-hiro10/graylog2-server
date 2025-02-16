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
package org.graylog2.jersey;

import com.google.common.collect.ImmutableMap;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.Provider;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

@Provider
public class PrefixAddingModelProcessor implements ModelProcessor {
    private final Map<String, String> packagePrefixes;

    public PrefixAddingModelProcessor(Map<String, String> packagePrefixes) {
        this.packagePrefixes = ImmutableMap.copyOf(packagePrefixes);
    }

    @Override
    public ResourceModel processResourceModel(ResourceModel model, Configuration config) {
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

        // Create new resource model.
        final ResourceModel.Builder resourceModelBuilder = new ResourceModel.Builder(false);
        for (final Resource resource : model.getResources()) {
            for (Class handlerClass : resource.getHandlerClasses()) {
                final String packageName = handlerClass.getPackage().getName();

                final Optional<String> packagePrefix = packagePrefixes.entrySet().stream()
                        .sorted((o1, o2) -> -o1.getKey().compareTo(o2.getKey()))
                        .filter(entry -> packageName.startsWith(entry.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst();

                if (packagePrefix.isPresent()) {
                    final String prefixedPath = prefixPath(packagePrefix.get(), resource.getPath());
                    final Resource newResource = Resource.builder(resource)
                            .path(prefixedPath)
                            .build();

                    resourceModelBuilder.addResource(newResource);
                } else {
                    resourceModelBuilder.addResource(resource);
                }
            }
        }

        return resourceModelBuilder.build();
    }

    private String prefixPath(String prefix, String path) {
        final String sanitizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        final String sanitizedPath = path.startsWith("/") ? path.substring(1) : path;
        return sanitizedPrefix + sanitizedPath;
    }

    @Override
    public ResourceModel processSubResource(ResourceModel model, Configuration config) {
        return model;
    }
}