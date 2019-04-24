package group.genco.onecloud.cloud;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Collectors;

public final class Cloud {

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private List<iDrive> _drives;

    public Cloud() {
        _drives = new ArrayList<>();
    }

    public List<File> list(String path) {
        final List<File> files = new ArrayList<>();
        _drives.parallelStream().forEach(D -> files.addAll(D.list(path)));
        List<File> filteredFiles = files.stream().filter(F -> F.getType() == 0).filter(distinctByKey(File::getName)).sorted(Comparator.comparing(File::getName)).collect(Collectors.toList());//addFolders
        filteredFiles.addAll(files.stream().filter(F -> F.getType() != 0).sorted(Comparator.comparing(File::getName)).collect(Collectors.toList()));// except Folders
        //Исключение дубликатов названий папок
        return filteredFiles;
    }

    public void add(iDrive drive) {
        _drives.add(drive);
    }

    public boolean rmUser(String userName) {
        return _drives.removeIf(D -> D.getUserName() == userName);
    }

    public void rm(String filePath) {
        _drives.forEach(D -> D.rm(filePath));
    }

    public void download(String filePath, String realPath) {
        _drives.forEach(D -> D.download(filePath, realPath));
    }

    public Long size() {
        return _drives.stream().mapToLong(D -> D.size()).sum();
    }

    public Long free() {
        return _drives.stream().mapToLong(D -> D.free()).sum();
    }

    public int count() {
        return _drives.size();
    }


    public List<iDrive> getDrives() {
        return _drives;
    }
}
