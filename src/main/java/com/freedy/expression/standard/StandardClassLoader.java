package com.freedy.expression.standard;

import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.utils.Color;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Freedy
 * @date 2022/9/30 22:51
 */
public class StandardClassLoader extends URLClassLoader {

    public String repositoryPath = System.getProperty("user.home") + "\\.m2\\repository\\";


    public StandardClassLoader() {
        super(new URL[]{}, CustomJavaCompiler.getSelfClassLoader());
    }


    public Set<Path> addPath(String path) throws Exception {
        path = path.strip();
        Path p = Path.of(path);
        HashSet<Path> list = new HashSet<>();
        if (!p.subpath(p.getNameCount() - 1, p.getNameCount()).toString().equals("pom.xml")) {
            if (Files.exists(p)) addURL(p.toUri().toURL());
            else list.add(p);
            return list;
        }
        addPomPath(p, null, list, new HashSet<>());
        return list;
    }

    private MavenProject addPomPath(Path pom, MavenProject project, Set<Path> failSet, Set<Path> successSet) throws IOException, XmlPullParserException {
        Model model = getModel(pom);

        if (project == null) {
            project = new MavenProject(model);
        }
        Parent parent = model.getParent();
        if (parent != null) {
            if (model.getGroupId() == null) model.setGroupId(parent.getGroupId());
            if (model.getVersion() == null) model.setGroupId(parent.getVersion());
            Path pomPath = getPomPath(parent);
            if (!Files.exists(pomPath)) {
                failSet.add(pomPath);
            } else {
                project.parent = addPomPath(pomPath, null, failSet, successSet);
            }
        }


        for (Dependency dep : model.getDependencies()) {
            if (dep.isOptional() && Optional.ofNullable(dep.getScope()).orElse("runtime").matches("compile|provided")) {
                continue;
            }
            parsePomVariable(dep, project);
            Path pomPath = getPomPath(dep);
            if (!Files.exists(pomPath)) {
                failSet.add(pomPath);
                continue;
            }
            Path jarPath = pomPath.resolveSibling(dep.getArtifactId() + "-" + dep.getVersion() + ".jar");
            if (Files.exists(jarPath)) {
                if (successSet.add(jarPath)) {
                    addURL(jarPath.toUri().toURL());
                    System.out.println(Color.green("add jar to path -> " + jarPath.getFileName()));
                }
            }
            addPomPath(pomPath, project, failSet, successSet);
        }
        return project;
    }

    private static Model getModel(Path pomFile) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        return reader.read(new FileInputStream(pomFile.toFile()));
    }

    private final Pattern pattern = Pattern.compile("\\$\\{(.*)}");

    private void parsePomVariable(Dependency dep, MavenProject project) {
        Matcher matcher = pattern.matcher(Optional.ofNullable(dep.getGroupId()).orElse(""));
        if (matcher.find()) dep.setGroupId(project.getProperty(matcher.group(1)));
        String version = dep.getVersion();
        if (version == null) {
            dep.setVersion(project.getDependencyVersion(dep.getGroupId(), dep.getArtifactId()));
            return;
        }
        matcher = pattern.matcher(version);
        if (matcher.find()) dep.setVersion(project.getProperty(matcher.group(1)));
    }

    private Path getPomPath(String groupId, String artifactId, String versionId, String relevantPath) {
        Path rel, pomPath = Path.of(repositoryPath).resolve(groupId.replace(".", "\\") + "\\" + artifactId + "\\" + versionId + "\\");

        if (relevantPath != null && Files.exists(pomPath.resolve(relevantPath)) && Files.isRegularFile(rel = pomPath.resolve(relevantPath))) {
            return rel;
        }
        return pomPath.resolve(artifactId + "-" + versionId + ".pom");
    }

    private Path getPomPath(InputLocationTracker model) {
        if (model instanceof Dependency a) return getPomPath(a.getGroupId(), a.getArtifactId(), a.getVersion(), null);
        if (model instanceof Parent a)
            return getPomPath(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getRelativePath());
        throw new IllegalArgumentException("illegal input");
    }


    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            return StandardClassLoader.class.getClassLoader().loadClass(name);
        }
    }


    private class MavenProject {
        private final Map<String, String> versionManagement = new HashMap<>();
        private final Properties properties;
        MavenProject parent;


        public MavenProject(Model model) throws XmlPullParserException, IOException {
            Properties p = model.getProperties();
            p.setProperty("project.name", Optional.ofNullable(model.getName()).orElse(""));
            p.setProperty("project.groupId", Optional.ofNullable(model.getGroupId()).orElse(""));
            p.setProperty("project.version", Optional.ofNullable(model.getVersion()).orElse(""));
            properties = p;
            initManagement(model.getDependencyManagement());
        }

        private void initManagement(DependencyManagement management) throws XmlPullParserException, IOException {
            if (management == null) return;
            for (Dependency dep : management.getDependencies()) {
                if (dep.getVersion() == null || dep.getArtifactId() == null || dep.getGroupId() == null) continue;
                parsePomVariable(dep, this);
                if (versionManagement.put(dep.getGroupId() + "-" + dep.getArtifactId(), dep.getVersion()) != null)
                    continue;
                Path path = getPomPath(dep);
                if (!Files.exists(path)) continue;
                Model model = getModel(path);
                initManagement(model.getDependencyManagement());
            }
        }


        public String getDependencyVersion(String groupId, String artifactId) {
            return versionManagement.getOrDefault(groupId + "-" + artifactId, parent == null ? null : parent.getDependencyVersion(groupId, artifactId));
        }

        public String getProperty(String key) {
            return (String) properties.getOrDefault(key, parent == null ? null : parent.getProperty(key));
        }

    }

}
