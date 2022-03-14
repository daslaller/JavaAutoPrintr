package javaautoprintr;

import com.company.PrintPDF;
import javafx.concurrent.Task;

import javax.print.PrintException;
import javax.print.PrintService;
import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

public class DirectoryWatch {
    private static Set<Runnable> onCreateList = new LinkedHashSet<>();
    private static Set<Runnable> onDeleteList = new LinkedHashSet<>();

    private static Set<Runnable> onSuccessfullPrint = new LinkedHashSet<>();
    private static final Set<Runnable> onFailedPrint = new LinkedHashSet<>();
    public static WatchService watchService;
    public static Path changeFile;
    public static LinkedBlockingDeque<Path> changedFiles = new LinkedBlockingDeque<>();
    public static PrintService printer;

    static {
        onCreateList.add(() -> System.out.println("File created: " + changeFile));
        onCreateList.add(() -> changedFiles.add(changeFile));
        onDeleteList.add(() -> System.out.println("Deleted file"));
        onSuccessfullPrint.add(() -> System.out.println("Print done"));
        onFailedPrint.add(() -> System.out.println("Print failed"));
        onFailedPrint.add(() -> {
            try {
                throw new PrintException();
            } catch (PrintException e) {
                PrintPDF.hasError().printStackTrace();
            }
        });
    }


    public static void watchDirectory(Path dir) throws IOException {
        watchDirectory(dir, false, null);
    }

    @SuppressWarnings("rawtypes")
    public static Task watchDirectory(Path dir, boolean enablePDFPrinting, PrintPDF pdfPrint) throws IOException {
        if (dir.toFile().isDirectory()) {
            watchService = FileSystems.getDefault().newWatchService();
            dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
            return new Task<>() {
                @Override
                protected Object call() {

                    System.out.println("Hello");
                    try {
                        while (!Thread.interrupted()) {
                            WatchKey take = watchService.take();
                            take.pollEvents().forEach(watchEvent -> {
                                WatchEvent.Kind<?> kind = watchEvent.kind();

                                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                    changeFile = dir.resolve((Path) watchEvent.context());
                                    if (onCreateList != null && !onCreateList.isEmpty()) {
                                        onCreateList.stream().map(Thread::new)/*.peek(t -> t.setDaemon(true))*/.forEach(Thread::start);
                                    }
                                    if (enablePDFPrinting) {
                                        Runnable runnable = () -> {
                                            try {
                                                PrintPDF printPDF = (pdfPrint == null) ? new PrintPDF(printer) : pdfPrint;
                                                System.out.println("Using printer: " + printPDF.getPrintService());

                                                if (changeFile.toFile().exists() && changeFile.toString().endsWith(".pdf")) {
                                                    if (printPDF.printDocument(changeFile)) {
                                                        if (onSuccessfullPrint != null && !onSuccessfullPrint.isEmpty()) {
                                                            onSuccessfullPrint.stream().map(Thread::new)/*.peek(t -> t.setDaemon(true))*/.forEach(Thread::start);
                                                        }
                                                    } else if (!onFailedPrint.isEmpty()) {
                                                        onFailedPrint.stream().map(Thread::new)/*.peek(t -> t.setDaemon(true))*/.forEach(Thread::start);
                                                    }
                                                } else {
                                                    System.out.println("Couldnt verify file \n" + changeFile.toAbsolutePath());
                                                    System.out.println("Contained pdf? " + changeFile.toString().endsWith(".pdf"));
                                                }

                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        };
                                        Thread printThread = new Thread(runnable);
//                                        printThread.setDaemon(true);
                                        printThread.start();
                                    }
                                }
                                if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                    if (onDeleteList != null && !onDeleteList.isEmpty()) {
                                        onDeleteList.stream().map(Thread::new)/*.peek(t -> t.setDaemon(true))*/.forEach(Thread::start);
                                    }
                                }
                                if (!take.reset()) {
                                    System.out.println("reset not valid, aborting. Please reregister watcher");
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
        } else {
            System.out.println("Must be a directory!");
            return null;
        }


    }

    public static void cancel() throws IOException {
        watchService.close();
    }

    public static Set<Runnable> getOnSuccessfullPrint() {
        return onSuccessfullPrint;
    }

    public static void setOnSuccessfullPrint(Set<Runnable> onSuccessfullPrint) {
        DirectoryWatch.onSuccessfullPrint = onSuccessfullPrint;
    }

    public void addOnCreate(Runnable action) {
        onCreateList.add(action);
    }

    public void addOnDelete(Runnable action) {
        onCreateList.add(action);
    }


    public static Set<Runnable> getOnCreateList() {
        return onCreateList;
    }

    public static void setOnCreateList(Set<Runnable> onCreateList) {
        DirectoryWatch.onCreateList = onCreateList;
    }

    public static Set<Runnable> getOnDeleteList() {
        return onDeleteList;
    }

    public static void setOnDeleteList(Set<Runnable> onDeleteList) {
        DirectoryWatch.onDeleteList = onDeleteList;
    }
}
