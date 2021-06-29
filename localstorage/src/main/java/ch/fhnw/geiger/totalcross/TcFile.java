package ch.fhnw.geiger.totalcross;

import java.io.IOException;

/**
 * <p>Interface for compatibility class for file access.</p>
 */
public interface TcFile extends AutoCloseable {
  /**
   * <p>Reads all bytes of a file into a byte array.</p>
   *
   * @param fname the file to be read
   * @return the content as byte array
   * @throws IOException if anything goes wrong accessing the file
   */
  byte[] readAllBytes(String fname) throws Throwable;

  /**
   * <p>Writes a byte array to a file.</p>
   *
   * @param fname the file to be written
   * @param buf   the content of the file
   * @throws IOException if anything goes wrong
   */
  void writeAllBytes(String fname, byte[] buf) throws Throwable;
}
