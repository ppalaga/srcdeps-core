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
package org.srcdeps.core.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.srcdeps.core.BuildRequest.Verbosity;
import org.srcdeps.core.SrcVersion;
import org.srcdeps.core.config.scalar.Duration;
import org.srcdeps.core.config.tree.ListOfScalarsNode;
import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.ScalarNode;
import org.srcdeps.core.config.tree.Visitor;
import org.srcdeps.core.config.tree.impl.DefaultContainerNode;
import org.srcdeps.core.config.tree.impl.DefaultListOfScalarsNode;
import org.srcdeps.core.config.tree.impl.DefaultScalarNode;
import org.srcdeps.core.config.tree.walk.TreeWalker;
import org.srcdeps.core.util.Equals.EqualsImplementations;

/**
 * A configuration suitable for a build system wanting to handle source dependencies.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Configuration {
    public static class Builder extends DefaultContainerNode<Node> {

        final BuilderIo.Builder builderIo = BuilderIo.builder();
        final ScalarNode<SrcVersion> buildRef = new DefaultScalarNode<>("buildRef", SrcVersion.getBranchMaster());
        final ScalarNode<Duration> buildTimeout = new DefaultScalarNode<>("buildTimeout", Duration.maxValue());
        final ScalarNode<Pattern> buildVersionPattern = new DefaultScalarNode<>("buildVersionPattern", null,
                Pattern.class, EqualsImplementations.equalsPattern());
        final ScalarNode<String> configModelVersion = new DefaultScalarNode<>("configModelVersion",
                LATEST_CONFIG_MODEL_VERSION);
        final ScalarNode<Boolean> forwardAsMasterConfig = new DefaultScalarNode<>("forwardAsMasterConfig",
                Boolean.FALSE);
        final ListOfScalarsNode<String> forwardProperties = new DefaultListOfScalarsNode<String>(
                FORWARD_PROPERTIES_ATTRIBUTE, String.class) {

            @Override
            public void applyDefaultsAndInheritance(Stack<Node> configurationStack) {
                if (getElements().isEmpty()) {
                    addAll(DEFAULT_FORWARD_PROPERTIES);
                }
            }

            @Override
            public boolean isInDefaultState(Stack<Node> configurationStack) {
                return getElements().isEmpty() || DEFAULT_FORWARD_PROPERTIES.equals(asSetOfValues());
            }

        };
        private Map<String, String> forwardPropertyValues = new TreeMap<>();
        final Maven.Builder maven = Maven.builder();
        final DefaultContainerNode<ScmRepository.Builder> repositories = new DefaultContainerNode<>("repositories");
        final ScalarNode<Boolean> skip = new DefaultScalarNode<>("skip", Boolean.FALSE);
        final ScalarNode<Path> sourcesDirectory = new DefaultScalarNode<>("sourcesDirectory", Path.class);
        final ScalarNode<Verbosity> verbosity = new DefaultScalarNode<>("verbosity", Verbosity.warn);

        private Builder() {
            super("srcdeps");
            addChildren( //
                    configModelVersion, //
                    forwardProperties, //
                    forwardAsMasterConfig, //
                    builderIo, //
                    skip, //
                    sourcesDirectory, //
                    verbosity, //
                    buildTimeout, //
                    buildRef, //
                    buildVersionPattern, //
                    maven, //
                    repositories //
            );
        }

        public Builder accept(Visitor visitor) {
            new TreeWalker().walk(this, visitor);
            return this;
        }

        public Configuration build() {
            Collection<ScmRepository.Builder> repoBuilders = repositories.getChildren().values();
            List<ScmRepository> repos = new ArrayList<>(repoBuilders.size());
            for (ScmRepository.Builder repoBuilder : repoBuilders) {
                repos.add(repoBuilder.build());
            }

            Map<String, String> useFwdPropValues = Collections.unmodifiableMap(forwardPropertyValues);
            this.forwardPropertyValues = null;

            Configuration result = new Configuration( //
                    configModelVersion.getValue(), //
                    forwardAsMasterConfig.getValue(), Collections.unmodifiableList(repos), //
                    sourcesDirectory.getValue(), //
                    skip.getValue(), //
                    builderIo.build(), //
                    forwardProperties.asSetOfValues(), //
                    useFwdPropValues, //
                    maven.build() //
            );
            return result;
        }

        public Builder builderIo(BuilderIo.Builder builderIo) {
            this.builderIo.init(builderIo);
            return this;
        }

        public Builder buildRef(SrcVersion value) {
            this.buildRef.setValue(value);
            return this;
        }

        public Builder buildTimeout(Duration buildTimeout) {
            this.buildTimeout.setValue(buildTimeout);
            return this;
        }

        public Builder buildVersionPattern(Pattern value) {
            this.buildVersionPattern.setValue(value);
            return this;
        }

        public Builder commentBefore(String value) {
            commentBefore.add(value);
            return this;
        }

        public Builder configModelVersion(String configModelVersion) {
            if (!SUPPORTED_CONFIG_MODEL_VERSIONS.contains(configModelVersion)) {
                throw new IllegalArgumentException(
                        String.format("Cannot parse configModelVersion [%s]; expected any of [%s]", configModelVersion,
                                SUPPORTED_CONFIG_MODEL_VERSIONS));
            }
            this.configModelVersion.setValue(configModelVersion);
            return this;
        }

        public Builder forwardAsMasterConfig(boolean value) {
            this.forwardAsMasterConfig.setValue(value);
            return this;
        }

        public Builder forwardProperties(Collection<String> names) {
            forwardProperties.addAll(names);
            return this;
        }

        public Builder forwardProperty(String name) {
            forwardProperties.add(name);
            return this;
        }

        public Builder forwardPropertyValue(String key, String value) {
            forwardPropertyValues.put(key, value);
            return this;
        }

        @Override
        public Map<String, Node> getChildren() {
            return children;
        }

        public Builder maven(Maven.Builder maven) {
            this.maven.init(maven);
            return this;
        }

        public Builder repositories(Map<String, ScmRepository.Builder> repoBuilders) {
            for (Map.Entry<String, ScmRepository.Builder> en : repoBuilders.entrySet()) {
                ScmRepository.Builder repoBuilder = en.getValue();
                repoBuilder.id(en.getKey());
                this.repositories.addChild(repoBuilder);
            }
            return this;
        }

        public Builder repository(ScmRepository.Builder repo) {
            this.repositories.addChild(repo);
            return this;
        }

        public Builder skip(boolean value) {
            this.skip.setValue(value);
            return this;
        }

        public Builder sourcesDirectory(Path value) {
            this.sourcesDirectory.setValue(value);
            return this;
        }

        public Builder verbosity(Verbosity value) {
            this.verbosity.setValue(value);
            return this;
        }

    }

    private static final Set<String> DEFAULT_FORWARD_PROPERTIES;
    private static final String FORWARD_PROPERTIES_ATTRIBUTE = "forwardProperties";

    private static final String LATEST_CONFIG_MODEL_VERSION = "2.5";

    private static final String SRCDEPS_ENCODING_PROPERTY = "srcdeps.encoding";
    private static final String SRCDEPS_MASTER_CONFIG_PROPERTY = "srcdeps.masterConfig";

    private static final Set<String> SUPPORTED_CONFIG_MODEL_VERSIONS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("2.0", "2.1", "2.2", "2.3", "2.4", LATEST_CONFIG_MODEL_VERSION)));

    static {
        DEFAULT_FORWARD_PROPERTIES = Collections.unmodifiableSet(new LinkedHashSet<>(
                Arrays.asList(SRCDEPS_MASTER_CONFIG_PROPERTY, Maven.getSrcdepsMavenPropertiesPattern())));
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the {@link Set} of system property names to use as {@code forwardProperties}
     */
    public static Set<String> getDefaultForwardProperties() {
        return DEFAULT_FORWARD_PROPERTIES;
    }

    /**
     * @return {@value Configuration#FORWARD_PROPERTIES_ATTRIBUTE} - the name of an attribute found inside
     *         {@code srcdeps.yaml}.
     */
    public static String getForwardPropertiesAttribute() {
        return FORWARD_PROPERTIES_ATTRIBUTE;
    }

    /**
     * @return the latest and default {@code configModelVersion}
     */
    public static String getLatestConfigModelVersion() {
        return LATEST_CONFIG_MODEL_VERSION;
    }

    /**
     * @return the name of the system property that can be used to set an encoding other than the default {@code utf-8}
     *         for reading any srcdeps related content, esp. the {@code srcdeps.yaml} file.
     */
    public static String getSrcdepsEncodingProperty() {
        return SRCDEPS_ENCODING_PROPERTY;
    }

    /**
     * @return {@value Configuration#SRCDEPS_MASTER_CONFIG_PROPERTY} - the name of the system property that can be used
     *         to replace any {@code srcdeps.yaml} configuration present in dependency source trees.
     */
    public static String getSrcdepsMasterConfigProperty() {
        return SRCDEPS_MASTER_CONFIG_PROPERTY;
    }

    /**
     * @return a {@link Set} of supported {@code configModelVersion}s
     */
    public static Set<String> getSupportedConfigModelVersions() {
        return SUPPORTED_CONFIG_MODEL_VERSIONS;
    }

    private final String configModelVersion;
    private final boolean forwardAsMasterConfig;
    private final Set<String> forwardProperties;
    private final transient Map<String, String> forwardPropertyValues;
    private final Maven maven;
    private final List<ScmRepository> repositories;
    private final boolean skip;
    private final Path sourcesDirectory;

    private Configuration(String configModelVersion, boolean forwardAsMasterConfig, List<ScmRepository> repositories,
            Path sourcesDirectory, boolean skip, BuilderIo redirects, Set<String> forwardPropertyNames,
            Map<String, String> forwardProperties, Maven maven) {
        super();
        this.configModelVersion = configModelVersion;
        this.forwardAsMasterConfig = forwardAsMasterConfig;
        this.repositories = repositories;
        this.sourcesDirectory = sourcesDirectory;
        this.skip = skip;
        this.forwardProperties = forwardPropertyNames;
        this.forwardPropertyValues = forwardProperties;
        this.maven = maven;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Configuration other = (Configuration) obj;
        if (configModelVersion == null) {
            if (other.configModelVersion != null)
                return false;
        } else if (!configModelVersion.equals(other.configModelVersion))
            return false;
        if (forwardAsMasterConfig != other.forwardAsMasterConfig)
            return false;
        if (forwardProperties == null) {
            if (other.forwardProperties != null)
                return false;
        } else if (!forwardProperties.equals(other.forwardProperties))
            return false;
        if (maven == null) {
            if (other.maven != null)
                return false;
        } else if (!maven.equals(other.maven))
            return false;
        if (repositories == null) {
            if (other.repositories != null)
                return false;
        } else if (!repositories.equals(other.repositories))
            return false;
        if (skip != other.skip)
            return false;
        if (sourcesDirectory == null) {
            if (other.sourcesDirectory != null)
                return false;
        } else if (!sourcesDirectory.equals(other.sourcesDirectory))
            return false;
        return true;
    }

    /**
     * @return the version of the configuration model
     */
    public String getConfigModelVersion() {
        return configModelVersion;
    }

    /**
     * Returns a set of property names that the top level builder A should pass as java system properties to every
     * dependency builder B (using {@code -DmyProperty=myValue}) command line arguments. Further, in case a child
     * builder B spawns its own new child builder C, B must pass all these properties to C in the very same manner as A
     * did to B.
     * <p>
     * A property name may end with asterisk {@code *} to denote that all properties starting with the part before the
     * asterisk should be forwared. E.g. {@code my.prop.*} would forward both {@code my.prop.foo} and
     * {@code my.prop.bar}.
     *
     * @return a {@link Set} of property names
     */
    public Set<String> getForwardProperties() {
        return forwardProperties;
    }

    /**
     * A map of property values that the top level builder A should pass as java system properties to every dependency
     * builder B using {@code -DmyProperty=myValue} style command line arguments. Further, in case a child builder B
     * spawns its own new child builder C, B must pass all these properties to C in the very same manner as A did to B.
     *
     * @return a {@link Map} of properties to forward
     */
    public Map<String, String> getForwardPropertyValues() {
        return forwardPropertyValues;
    }

    /**
     * @return Maven specific settings that apply both on the top level and for each source repository (unless they are
     *         overriden at the repository level).
     */
    public Maven getMaven() {
        return maven;
    }

    /**
     * @return the list of {@link ScmRepository ScmRepositories} that should be used for building of the dependencies
     */
    public List<ScmRepository> getRepositories() {
        return repositories;
    }

    /**
     * Returns a {@link Path} to the sources directory where the dependency sources should be checked out. Each SCM
     * repository will have a subdirectory named after its {@code id} there.
     *
     * @return a {@link Path} to the sources
     */
    public Path getSourcesDirectory() {
        return sourcesDirectory;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((configModelVersion == null) ? 0 : configModelVersion.hashCode());
        result = prime * result + (forwardAsMasterConfig ? 1231 : 1237);
        result = prime * result + ((forwardProperties == null) ? 0 : forwardProperties.hashCode());
        result = prime * result + ((maven == null) ? 0 : maven.hashCode());
        result = prime * result + ((repositories == null) ? 0 : repositories.hashCode());
        result = prime * result + (skip ? 1231 : 1237);
        result = prime * result + ((sourcesDirectory == null) ? 0 : sourcesDirectory.hashCode());
        return result;
    }

    public boolean isForwardAsMasterConfig() {
        return forwardAsMasterConfig;
    }

    /**
     * @return {@code true} if the whole srcdeps processing should be skipped or {@code false} otherwise
     */
    public boolean isSkip() {
        return skip;
    }

    @Override
    public String toString() {
        return "Configuration [configModelVersion=" + configModelVersion + ", forwardAsMasterConfig="
                + forwardAsMasterConfig + ", forwardPropertyNames=" + forwardProperties + ", maven=" + maven
                + ", repositories=" + repositories + ", skip=" + skip + ", sourcesDirectory=" + sourcesDirectory + "]";
    }

}
