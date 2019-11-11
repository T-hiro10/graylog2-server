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

package org.graylog2.indexer.retention.strategies;

import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.periodical.IndexRetentionThread;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public abstract class AbstractIndexCountBasedRetentionStrategy implements RetentionStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIndexCountBasedRetentionStrategy.class);

    private final Indices indices;
    private final ActivityWriter activityWriter;

    public AbstractIndexCountBasedRetentionStrategy(Indices indices,
                                                    ActivityWriter activityWriter) {
        this.indices = requireNonNull(indices);
        this.activityWriter = requireNonNull(activityWriter);
    }

    protected abstract Optional<Integer> getMaxNumberOfIndices(IndexSet indexSet);
    protected abstract void retain(String indexName, IndexSet indexSet);

    @Override
    public void retain(IndexSet indexSet) {
        final Map<String, Set<String>> deflectorIndices = indexSet.getAllIndexAliases();
        final int indexCount = (int)deflectorIndices.keySet()
            .stream()
            .filter(indexName -> !indices.isReopened(indexName))
            .count();

        final Optional<Integer> maxIndices = getMaxNumberOfIndices(indexSet);

        if (!maxIndices.isPresent()) {
            LOG.warn("No retention strategy configuration found, not running index retention!");
            return;
        }

        // Do we have more indices than the configured maximum?
        if (indexCount <= maxIndices.get()) {
            LOG.debug("Number of indices ({}) lower than limit ({}). Not performing any retention actions.",
                    indexCount, maxIndices.get());
            return;
        }

        // We have more indices than the configured maximum! Remove as many as needed.
        final int removeCount = indexCount - maxIndices.get();
        final String msg = "Number of indices (" + indexCount + ") higher than limit (" + maxIndices.get() + "). " +
                "Running retention for " + removeCount + " indices.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, IndexRetentionThread.class));

        runRetention(indexSet, deflectorIndices, removeCount);
    }

    private void runRetention(IndexSet indexSet, Map<String, Set<String>> deflectorIndices, int removeCount) {
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

        final Set<String> orderedIndices = Arrays.stream(indexSet.getManagedIndices())
            .sorted((indexName1, indexName2) -> indexSet.extractIndexNumber(indexName2).orElse(0).compareTo(indexSet.extractIndexNumber(indexName1).orElse(0)))
            .filter(indexName -> !indices.isReopened(indexName))
            .filter(indexName -> !(deflectorIndices.getOrDefault(indexName, Collections.emptySet()).contains(indexSet.getWriteIndexAlias())))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        orderedIndices
            .stream()
            .skip(orderedIndices.size() - removeCount)
            .forEach(indexName -> {
                final String strategyName = this.getClass().getCanonicalName();
                final String msg = "Running retention strategy [" + strategyName + "] for index <" + indexName + ">";
                LOG.info(msg);
                activityWriter.write(new Activity(msg, IndexRetentionThread.class));

                // Sorry if this should ever go mad. Run retention strategy!
                retain(indexName, indexSet);
            });
    }
}
