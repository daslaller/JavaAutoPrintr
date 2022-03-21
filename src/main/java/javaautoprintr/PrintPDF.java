package javaautoprintr;

import com.company.JFXOptionPane;
import javafx.application.Platform;
import org.pdfbox.pdmodel.PDDocument;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.*;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class PrintPDF {
    static { /* works fine! ! */
        System.setProperty("java.awt.headless", "false");
        System.out.println(java.awt.GraphicsEnvironment.isHeadless());
        /* ---> prints true */
    }

    public static PrintService[] availableServices = PrintServiceLookup.lookupPrintServices(null, null);
    PrintService selectedService;
    PrinterJob job;
    boolean userPrompted = false;
    public static Throwable cause;

    public PrintPDF(PrintService selectedService) {
        if (selectedService == null) {
            System.out.println("Printservice cannot be null! ");
        }
        this.selectedService = Objects.requireNonNull(selectedService, "Is still null! Do not initiate a print job before a printer is set!");
    }

    public PrintPDF() {
        this(PrintServiceLookup.lookupDefaultPrintService());
    }

    public PrintService displayPrinterSelector() {
        selectedService = dialog();
        userPrompted = true;
        return selectedService;
    }

    public static PrintService dialog() {
        if (Platform.isFxApplicationThread()) {
            return JFXOptionPane.showChoiceDialog(availableServices, "Välj skrivare", "Välj en skrivare i listan", "Tillgängliga skrivare:", PrintServiceLookup.lookupDefaultPrintService());
        } else {
            return (PrintService) JOptionPane.showInputDialog(null, "Välj skrivare", "Val av skrivare", JOptionPane.PLAIN_MESSAGE, null, availableServices, PrintServiceLookup.lookupDefaultPrintService());
        }

    }

    public void setSelectedService(PrintService newSelectedService) {
        selectedService = newSelectedService;
        userPrompted = true;
    }

    public boolean printDocument(PDDocument document) {

        if (!userPrompted && selectedService == null) {
            displayPrinterSelector();
        }
        job = PrinterJob.getPrinterJob();
        job.setPageable(document);
        System.out.println("Initiated print job: " + getPrintJob());
        try {
            job.setPrintService(selectedService);
            job.print();
            document.close();
            System.out.println("Printed fine");
            return true;
        } catch (PrinterException | IOException e) {
            e.printStackTrace();
            cause = e.getCause();
            System.out.println(cause.toString());
            System.out.println("Failed print: " + selectedService);
            return false;
        }

    }

    public static boolean isError() {
        return cause != null;
    }

    public static Throwable hasError() {
        if (isError()) {
            System.out.println("Has error!");
            return cause;
        } else {
            return null;
        }
    }

    public boolean printDocument(Path pathToFile) throws IOException {
        if (!pathToFile.toString().endsWith(".pdf")) {
            System.out.println("Didnt end with pdf");
            return false;
        }
        return printDocument(PDDocument.load(pathToFile.toFile()));
    }

    public PrintService getPrintService() {
        return selectedService;
    }

    public PrinterJob getPrintJob() {
        return job;
    }
}
