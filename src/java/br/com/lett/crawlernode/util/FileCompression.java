package br.com.lett.crawlernode.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.session.Session;

public class FileCompression {

  private FileCompression() {}

  private static final Logger logger = LoggerFactory.getLogger(FileCompression.class);

  public static void compressToTar(String name, List<String> files) throws IOException {
    try (TarArchiveOutputStream out = getTarArchiveOutputStream(name)) {
      for (String file : files) {
        addToArchiveTARCompression(out, new File(file));
      }
    }
  }

  private static TarArchiveOutputStream getTarArchiveOutputStream(String name) throws IOException {
    return new TarArchiveOutputStream(new FileOutputStream(name));
  }

  private static void addToArchiveTARCompression(TarArchiveOutputStream out, File file) throws IOException {
    String entry = file.getName();
    if (file.isFile()) {
      out.putArchiveEntry(new TarArchiveEntry(file, entry));
      try (FileInputStream in = new FileInputStream(file)) {
        IOUtils.copy(in, out);
      }
      out.closeArchiveEntry();
      file.delete();
    } else if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          addToArchiveTARCompression(out, child);
          child.delete();
        }
      }
    }
  }

  public static void decompressTARFile(String in, File out) throws IOException {
    try (TarArchiveInputStream fin = new TarArchiveInputStream(new FileInputStream(in))) {
      TarArchiveEntry entry;
      while ((entry = fin.getNextTarEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }
        File curfile = new File(out, entry.getName());
        File parent = curfile.getParentFile();
        if (!parent.exists()) {
          parent.mkdirs();
        }
        IOUtils.copy(fin, new FileOutputStream(curfile));
      }
    }
  }

  public static void compressFileToGZIP(String toCompressFile, String compressedFile, boolean mustDeleteFile, Session session) {

    byte[] buffer = new byte[1024];

    File file = null;
    try {

      file = new File(toCompressFile);
      GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(compressedFile));
      FileInputStream stream = new FileInputStream(file);

      int len;
      while ((len = stream.read(buffer)) > 0) {
        gzos.write(buffer, 0, len);
      }

      stream.close();

      gzos.finish();
      gzos.close();


    } catch (Exception ex) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(ex));
    } finally {
      if (file != null && mustDeleteFile) {
        file.delete();
      }
    }
  }
}

