package com.ai.southernquiet.session;

import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.PathMeta;
import com.ai.southernquiet.filesystem.PathNotFoundException;
import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于{@link com.ai.southernquiet.filesystem.FileSystem}的Jetty Session持久化.
 */
@Component
public class FileSessionDataStore extends AbstractSessionDataStore {
    public final static String DEFAULT_ROOT = "SESSION";
    public final static String NAME_SEPARATOR = "__";

    private FileSystem fileSystem = null;
    private String workingRoot;

    public FileSessionDataStore() {
        this(DEFAULT_ROOT);
    }

    public FileSessionDataStore(String workingRoot) {
        this.workingRoot = workingRoot;
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    @Autowired
    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public String getWorkingRoot() {
        return workingRoot;
    }

    public void setWorkingRoot(String workingRoot) {
        this.workingRoot = workingRoot;
    }

    @Override
    public void doStore(String id, SessionData data, long lastSaveTime) throws Exception {
        fileSystem.put(getFilePath(id, data.getExpiry()), SessionJSON.dataToJSON(data));
    }

    @Override
    public Set<String> doGetExpired(Set<String> candidates) {
        long now = System.currentTimeMillis();

        Set<String> validCandidates = candidates.stream()
            .map(id -> getIdPrefix(id))
            .map(prefix -> {
                try {
                    return fileSystem.files(getWorkingRoot(), prefix);
                }
                catch (PathNotFoundException e) {
                    throw new RuntimeException(e);
                }
            })
            .flatMap(metas -> metas.stream().map(PathMeta::getName))
            .filter(name -> getExpiryFromFileName(name) > now)
            .map(name -> getIdFromFileName(name))
            .collect(Collectors.toSet());

        candidates.removeAll(validCandidates);
        return candidates;
    }

    @Override
    public boolean isPassivating() {
        return true;
    }

    @Override
    public boolean exists(String id) throws Exception {
        long now = System.currentTimeMillis();

        return fileSystem.files(getWorkingRoot(), getIdPrefix(id)).stream()
            .anyMatch(meta -> getExpiryFromFileName(meta.getName()) > now);
    }

    @Override
    public SessionData load(String id) throws Exception {
        Optional<PathMeta> opt = fileSystem.files(getWorkingRoot(), getIdPrefix(id)).stream().findFirst();
        if (opt.isPresent()) {
            String json = fileSystem.readString(opt.get().getPath());
            return SessionJSON.jsonToData(json);
        }

        return null;
    }

    @Override
    public boolean delete(String id) throws Exception {
        Optional<PathMeta> opt = fileSystem.files(getWorkingRoot(), getIdPrefix(id)).stream().findFirst();
        if (opt.isPresent()) {
            fileSystem.delete(opt.get().getPath());
        }

        return true;
    }

    private String getIdPrefix(String sessionId) {
        return sessionId + NAME_SEPARATOR;
    }

    private String getFileName(String sessionId, long expiry) {
        return sessionId + NAME_SEPARATOR + expiry;
    }

    private String getFilePath(String sessionId, long expiry) {
        return getWorkingRoot() + FileSystem.PATH_SEPARATOR + getFileName(sessionId, expiry);
    }

    private String getIdFromFileName(String name) {
        return name.substring(0, name.indexOf(NAME_SEPARATOR));
    }

    private long getExpiryFromFileName(String name) {
        return Long.parseLong(name.substring(name.indexOf(NAME_SEPARATOR) + NAME_SEPARATOR.length()));
    }
}
