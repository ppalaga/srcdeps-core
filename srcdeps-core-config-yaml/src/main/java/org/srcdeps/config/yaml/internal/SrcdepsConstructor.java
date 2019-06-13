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
package org.srcdeps.config.yaml.internal;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.srcdeps.core.SrcVersion;
import org.srcdeps.core.config.BuilderIo;
import org.srcdeps.core.config.Configuration;
import org.srcdeps.core.config.Maven;
import org.srcdeps.core.config.MavenAssertions;
import org.srcdeps.core.config.ScmRepository;
import org.srcdeps.core.config.ScmRepositoryGradle;
import org.srcdeps.core.config.ScmRepositoryMaven;
import org.srcdeps.core.config.scalar.CharStreamSource;
import org.srcdeps.core.config.scalar.Duration;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.ScalarNode;

/**
 * Extends {@link Constructor} through adding a support for {@link Path} and our custom {@link BuilderPropertyUtils}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class SrcdepsConstructor extends Constructor {
    private class PathConstruct extends ConstructScalar {
        @Override
        public Object construct(Node node) {
            final Class<? extends Object> type = node.getType();
            if (type == Path.class) {
                return Paths.get(((ScalarNode) node).getValue());
            } else if (type == Duration.class) {
                return Duration.of(((ScalarNode) node).getValue());
            } else if (type == CharStreamSource.class) {
                return CharStreamSource.of(((ScalarNode) node).getValue());
            } else if (type == SrcVersion.class) {
                return SrcVersion.parseRef(((ScalarNode) node).getValue());
            } else if (type == Pattern.class) {
                return Pattern.compile(((ScalarNode) node).getValue());
            } else if (type == Charset.class) {
                return Charset.forName(((ScalarNode) node).getValue());
            } else {
                return super.construct(node);
            }
        }
    }

    public SrcdepsConstructor() {
        super();
        this.yamlClassConstructors.put(NodeId.scalar, new PathConstruct());
        this.setPropertyUtils(new BuilderPropertyUtils(Configuration.Builder.class, BuilderIo.Builder.class,
                Maven.Builder.class, MavenAssertions.FailWithoutBuilder.class, MavenAssertions.FailWithBuilder.class,
                ScmRepository.Builder.class, ScmRepositoryMaven.Builder.class, ScmRepositoryGradle.Builder.class));

    }

}