/**
 * Copyright 2015-2016 Maven Source Dependencies
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.srcdeps.core.config.tree.ListOfScalarsNode;
import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.ScalarNode;
import org.srcdeps.core.config.tree.impl.DefaultContainerNode;
import org.srcdeps.core.config.tree.impl.DefaultListOfScalarsNode;
import org.srcdeps.core.config.tree.impl.DefaultScalarNode;

/**
 * A configuration node to define conditions under which the outer Maven build that has source dependencies should fail.
 * Note that these settings are effective only if source dependencies exist. In case there are no source dependencies in
 * the outer project, the build will not fail even if any of the goals, profiles or properties are present. This is esp.
 * handy if you want to avoid releases with source dependencies.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class MavenFailWith {

    public static class Builder extends DefaultContainerNode<Node> {

        ScalarNode<Boolean> addDefaults = new DefaultScalarNode<>("addDefaults", Boolean.TRUE);
        ListOfScalarsNode<String> goals = new DefaultListOfScalarsNode<String>("goals", String.class) {

            @Override
            public void applyDefaultsAndInheritance(Stack<Node> configurationStack) {
                Builder.this.addDefaults.applyDefaultsAndInheritance(configurationStack);
                if (Builder.this.addDefaults.getValue() && getElements().isEmpty()) {
                    addAll(DEFAULT_FAIL_GOALS);
                }
            }

            @Override
            public boolean isInDefaultState(Stack<Node> configurationStack) {
                Boolean addDefaultsValue = Builder.this.addDefaults.getValue();
                return getElements().isEmpty() || ((addDefaultsValue == null || Boolean.TRUE.equals(addDefaultsValue))
                        && DEFAULT_FAIL_GOALS.equals(asSetOfValues()));
            }

        };
        ListOfScalarsNode<String> profiles = new DefaultListOfScalarsNode<>("profiles", String.class);
        ListOfScalarsNode<String> properties = new DefaultListOfScalarsNode<>("properties", String.class);

        public Builder() {
            super("failWith");
            addChildren(addDefaults, goals, profiles, properties);
        }

        public Builder addDefaults(boolean addDefaults) {
            this.addDefaults.setValue(addDefaults);
            return this;
        }

        public MavenFailWith build() {
            MavenFailWith result = new MavenFailWith( //
                    addDefaults.getValue(), //
                    goals.asSetOfValues(), //
                    profiles.asSetOfValues(), //
                    properties.asSetOfValues() //
            );
            this.goals = null;
            this.profiles = null;
            this.properties = null;
            return result;
        }

        @Override
        public Map<String, Node> getChildren() {
            return children;
        }

        public Builder goal(String goal) {
            this.goals.add(goal);
            return this;
        }

        public Builder goals(Collection<String> goals) {
            this.goals.addAll(goals);
            return this;
        }

        public Builder profile(String profile) {
            this.profiles.add(profile);
            return this;
        }

        public Builder profiles(Collection<String> profiles) {
            this.profiles.addAll(profiles);
            return this;
        }

        public Builder properties(Collection<String> properties) {
            this.properties.addAll(properties);
            return this;
        }

        public Builder property(String property) {
            this.properties.add(property);
            return this;
        }

    }

    private static Set<String> DEFAULT_FAIL_GOALS = Collections
            .unmodifiableSet(new LinkedHashSet<>(Arrays.asList("release:prepare", "release:perform")));

    public static Builder builder() {
        return new Builder();
    }

    public static Set<String> getDefaultFailGoals() {
        return DEFAULT_FAIL_GOALS;
    }

    private final boolean addDefaults;

    private final Set<String> goals;
    private final Set<String> profiles;

    private final Set<String> properties;

    private MavenFailWith(boolean addDefaults, Set<String> goals, Set<String> profiles, Set<String> properties) {
        super();
        this.addDefaults = addDefaults;
        this.goals = goals;
        this.profiles = profiles;
        this.properties = properties;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MavenFailWith other = (MavenFailWith) obj;
        if (addDefaults != other.addDefaults)
            return false;
        if (goals == null) {
            if (other.goals != null)
                return false;
        } else if (!goals.equals(other.goals))
            return false;
        if (profiles == null) {
            if (other.profiles != null)
                return false;
        } else if (!profiles.equals(other.profiles))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        return true;
    }

    /**
     * @return a {@link Set} of Maven goals. Presence of any of the returned goals will make the outer build fail. The
     *         goals can be either in the short form, e.g. {@code "release:prepare"}, or in the fully qualified form
     *         e.g. {@code "org.my-group:my-plugin:my-mojo"}
     */
    public Set<String> getGoals() {
        return goals;
    }

    /**
     * @return a {@link Set} of Maven profile IDs. Presence of any of the returned profile ID will make the outer build
     *         fail
     */
    public Set<String> getProfiles() {
        return profiles;
    }

    /**
     * @return a {@link Set} of Maven properties. Presence of any of the returned properties will make the outer build
     *         fail. The {@link Set} can contain either bare property names (in which case a mere property presence will
     *         make the build fail) or name=value pairs (in which case the build will only fail if the named property
     *         has the given value).
     */
    public Set<String> getProperties() {
        return properties;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (addDefaults ? 1231 : 1237);
        result = prime * result + ((goals == null) ? 0 : goals.hashCode());
        result = prime * result + ((profiles == null) ? 0 : profiles.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        return result;
    }

    /**
     * @return if {@code true} the respective defaults will be added to the {@link Set}s returned by
     *         {@link #getGoals()}, {@link #getProfiles()} and {@link #getProperties()}. Otherwise, the defaults will be
     *         disregared and only the {@link Set}s as returned by {@link #getGoals()}, {@link #getProfiles()} and
     *         {@link #getProperties()} will be effective.
     */
    public boolean isAddDefaults() {
        return addDefaults;
    }

    @Override
    public String toString() {
        return "MavenFailWith [addDefaults=" + addDefaults + ", goals=" + goals + ", profiles=" + profiles
                + ", properties=" + properties + "]";
    }

}
