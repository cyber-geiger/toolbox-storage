package ch.fhnw.geiger.totalcross;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * <p>A cross platform wrapper class for reading and writing byte streams.</p>
 */
public class File implements TcFile {

  private class TcWrapper implements TcFile {

    @Override
    public byte[] readAllBytes(String fname) throws IOException {
      try {
        Class cls = Class.forName("totalcross.io.File");
        int mode = cls.getField("READ_ONLY").getInt(cls);
        Object[] arglist = new Object[]{fname, mode};
        Class[] partypes = new Class[]{String.class, int.class};
        Constructor ct = cls.getConstructor(partypes);
        Object obj = (ct.newInstance(arglist));
        return (byte[]) (cls.getMethod("readAndClose").invoke(obj));
      } catch (InvocationTargetException | IllegalAccessException | InstantiationException
          | NoSuchMethodException | NoSuchFieldException | ClassNotFoundException e) {
        // FIXME insert proper logging/error handling (but should not be called)
      }
      return null;
    }

    @Override
    public void writeAllBytes(String fname, byte[] buf) throws IOException {
      try {
        Class cls = Class.forName("totalcross.io.File");
        Class[] partypes = new Class[]{String.class, int.class};
        Constructor ct = cls.getConstructor(partypes);
        Object[] arglist = new Object[]{fname, cls.getField("CREATE_EMPTY")};
        Object obj = (ct.newInstance(arglist));
        cls.getMethod("writeAndClose", byte[].class).invoke(obj, buf);
      } catch (InvocationTargetException | IllegalAccessException | InstantiationException
          | NoSuchMethodException | NoSuchFieldException | ClassNotFoundException e) {
        // FIXME insert proper logging/error handling (but should not be called)
      }
    }

    @Override
    public void close() throws Exception {
      // empty as we do not implement any methods leaving resources open
    }
  }

  private class JavaWrapper implements TcFile {

    @Override
    public byte[] readAllBytes(String fname) throws IOException {
      try {
        Object path = Class.forName("java.io.Paths").getMethod("get", String.class)
            .invoke(null, fname);
        Method ct = Class.forName("java.io.Files")
            .getMethod("readAllBytes", Class.forName("java.io.Path"));
        return (byte[]) (ct.invoke(null, path));
      } catch (Exception e) {
        // FIXME insert proper logging/error handling (but should not be called)
      }
      return null;
    }

    @Override
    public void writeAllBytes(String fname, byte[] buf) throws IOException {
      try {
        Object path = Class.forName("java.io.Paths").getMethod("get", String.class)
            .invoke(null, fname);
        Method ct = Class.forName("java.io.Files")
            .getMethod("write", Class.forName("java.io.Path"), byte[].class);
        ct.invoke(null, path, buf);
      } catch (Exception e) {
        // FIXME insert proper logging/error handling (but should not be called)
      }
    }

    @Override
    public void close() throws Exception {
      // empty as we do not implement any methods leaving resources open
    }
  }

  TcFile file;

  /**
   * Creates either a TotalCross-wrapper or a Java-wrapper.
   */
  public File() {
    if (Detector.isTotalCross()) {
      file = new File.TcWrapper();
    } else {
      file = new File.JavaWrapper();
    }
  }

  @Override
  public byte[] readAllBytes(String fname) throws Throwable {
    try {
      return file.readAllBytes(fname);
    } catch (Exception e) {
      throw (e.getCause() != null ? e.getCause() : e);
    }
  }

  @Override
  public void writeAllBytes(String fname, byte[] buf) throws Throwable {
    try {
      file.writeAllBytes(fname, buf);
    } catch (Exception e) {
      throw (e.getCause() != null ? e.getCause() : e);
    }
  }

  @Override
  public void close() throws Exception {
    // empty as we do not implement any methods leaving resources open
  }
}
