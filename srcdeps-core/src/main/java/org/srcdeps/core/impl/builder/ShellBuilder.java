/**
 * Copyright 2015-2019 Maven Source Dependencies
 * Plugin contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.srcdeps.core.impl.builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.srcdeps.core.BuildException;
import org.srcdeps.core.BuildRequest;
import org.srcdeps.core.BuildRequest.Verbosity;
import org.srcdeps.core.Builder;
import org.srcdeps.core.shell.Shell;
import org.srcdeps.core.shell.ShellCommand;

/**
 * A base class for command line build tools.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public abstract class ShellBuilder implements Builder {

    protected final String executable;

    /**
     * Set by {@link #setVersions(BuildRequest)}, tells in how many milliseconds must {@link #build(BuildRequest)}
     * finish
     */
    protected long restTimeoutMs = Long.MIN_VALUE;

    /**
     * @param executable the executable such as {@code mvn}
     */
    public ShellBuilder(String executable) {
        super();
        this.executable = executable;
    }

    @Override
    public void build(BuildRequest request) throws BuildException {

        /*
         * restTimeoutMs == Long.MIN_VALUE means that the restTimeoutMs was not set by setVersions() and it will
         * therefore be ignored
         */
        long timeoutMs = restTimeoutMs == Long.MIN_VALUE ? request.getTimeoutMs() : restTimeoutMs;

        List<String> args = mergeArguments(request);
        ShellCommand command = ShellCommand.builder() //
                .id(request.getScmRepositoryId()) //
                .executable(locateExecutable(request)) //
                .arguments(args) //
                .workingDirectory(request.getProjectRootDirectory()) //
                .environment(mergeEnvironment(request)) //
                .output(request.getOuput()) //
                .timeoutMs(timeoutMs) //
                .build();
        Shell.execute(command).assertSuccess();
    }

    protected abstract List<String> getDefaultBuildArguments();

    protected abstract Map<String, String> getDefaultBuildEnvironment();

    protected List<String> getForwardPropertiesArguments(final BuildRequest request) {
        final List<String> result = new ArrayList<>();
        request.getForwardPropertyValues().entrySet().forEach(e -> result.add("-D" + e.getKey() + "=" + e.getValue()));
        result.add("-Dsrcdeps.forwardProperties="
                + request.getForwardPropertyNames().stream().collect(Collectors.joining(",")));
        return result;
    }

    protected abstract List<String> getSkipTestsArguments(boolean skipTests);

    protected abstract List<String> getVerbosityArguments(Verbosity verbosity);

    /**
     * Always returns {@link #executable}. Subclasses may choose to return some thing else depending on the given
     * {@code request}.
     *
     * @param request the request to build
     * @return the path to the executable
     */
    protected String locateExecutable(BuildRequest request) {
        return executable;
    }

    /**
     * Returns a new {@link List} that contains build arguments combined from the following sources:
     * <ul>
     * <li>{@link #getDefaultBuildArguments()} (if {@code request.isAddDefaultBuildArguments()} return
     * {@code true})</li>
     * <li>{@code request.getBuildArguments()}</li>
     * <li>{@code getVerbosityArguments(request.getVerbosity()))}</li>
     * <li>{@code getForwardPropertiesArguments(request.getForwardProperties())}</li>
     * </ul>
     *
     * @param request the request for which we are merging the arguments
     * @return a new {@link List}, never {@code null}
     */
    protected List<String> mergeArguments(BuildRequest request) {
        List<String> result = new ArrayList<>();
        if (request.isAddDefaultBuildArguments()) {
            result.addAll(getDefaultBuildArguments());
        }
        result.addAll(request.getBuildArguments());
        result.addAll(getVerbosityArguments(request.getVerbosity()));
        result.addAll(getSkipTestsArguments(request.isSkipTests()));
        result.addAll(getForwardPropertiesArguments(request));
        return result;
    }

    /**
     * Returns a new {@link Map} that contains environment variables combined from the following sources (the entries
     * added at later stages win):
     * <ul>
     * <li>{@link #getDefaultBuildEnvironment()} (if {@code request.isAddDefaultBuildEnvironment()} returns
     * {@code true})</li>
     * <li>{@code request.getBuildEnvironment()}</li>
     * </ul>
     *
     * @param request the request for which we are merging the arguments
     * @return a new {@link Map}, never {@code null}
     */
    protected Map<String, String> mergeEnvironment(BuildRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        if (request.isAddDefaultBuildEnvironment()) {
            result.putAll(getDefaultBuildEnvironment());
        }
        result.putAll(request.getBuildEnvironment());
        return result;
    }
}
