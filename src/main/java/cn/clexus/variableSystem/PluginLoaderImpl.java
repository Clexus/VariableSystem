package cn.clexus.variableSystem;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

public class PluginLoaderImpl implements PluginLoader {
    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addDependency(new Dependency(new DefaultArtifact("com.zaxxer:HikariCP:7.0.2"),null));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.postgresql:postgresql:42.7.8"),null));
        resolver.addRepository(new RemoteRepository.Builder("central","default","https://maven.aliyun.com/repository/public/").build());
        classpathBuilder.addLibrary(resolver);
    }
}
