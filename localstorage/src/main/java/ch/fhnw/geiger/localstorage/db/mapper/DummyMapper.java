package ch.fhnw.geiger.localstorage.db.mapper;

import ch.fhnw.geiger.localstorage.SearchCriteria;
import ch.fhnw.geiger.localstorage.StorageController;
import ch.fhnw.geiger.localstorage.StorageException;
import ch.fhnw.geiger.localstorage.db.GenericController;
import ch.fhnw.geiger.localstorage.db.data.Node;
import ch.fhnw.geiger.localstorage.db.data.NodeImpl;
import ch.fhnw.geiger.localstorage.db.data.NodeValue;
import ch.fhnw.geiger.totalcross.ByteArrayInputStream;
import ch.fhnw.geiger.totalcross.ByteArrayOutputStream;
import ch.fhnw.geiger.totalcross.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * <p>A non-persisting dummy mapper for test purposes.</p>
 */
public class DummyMapper extends AbstractMapper {

  private final Map<String, Node> nodes = new HashMap<>();

  private StorageController controller = null;

  @Override
  public void setController(StorageController controller) {
    this.controller = controller;
  }

  @Override
  public Node get(String path) throws StorageException {
    checkPath(path);
    getSanity(path);
    Node ret = nodes.get(path);
    if (ret == null) {
      throw new StorageException("Node not found");
    }
    if (ret.isTombstone()) {
      Node returnNode = new NodeImpl(path, true);
      returnNode.setVisibility(ret.getVisibility());
      return returnNode;
    }
    return ret.deepClone();
  }

  @Override
  public void add(Node node) throws StorageException {
    checkPath(node);
    synchronized (nodes) {
      if (nodes.get(node.getPath()) != null) {
        throw new StorageException("Node does already exist");
      }
      if (node.isSkeleton()) {
        throw new StorageException("Skeleton nodes cannot be added.");
      }

      // check if parent node is available
      if (node.getParentPath() != null && !"".equals(node.getParentPath())) {
        if (nodes.get(node.getParentPath()) == null) {
          throw new StorageException("Parent node \"" + node.getParentPath() + "\" does not exist");
        }
        // add reference to parent
        nodes.get(node.getParentPath()).addChild(node);
      }
      // add node
      nodes.put(node.getPath(), node.shallowClone());
    }
  }

  @Override
  public void update(Node node) throws StorageException {
    checkPath(node);
    synchronized (nodes) {
      if (!"".equals(node.getParentPath()) && nodes.get(node.getParentPath()) == null) {
        throw new StorageException("Node does not exist");
      }
      nodes.get(node.getPath()).update(node);
    }
  }

  @Override
  public void rename(String oldPath, String newPath) throws StorageException {
    checkPath(oldPath);
    checkPath(newPath);
    synchronized (nodes) {
      if (nodes.get(oldPath) == null) {
        throw new StorageException("Node does not exist");
      }
      Node oldNode = nodes.get(oldPath);

      // clone node at new location
      NodeImpl newNode = new NodeImpl(newPath);

      // copy ordinals
      newNode.setOwner(oldNode.getOwner());
      newNode.setVisibility(oldNode.getVisibility());

      // copy values
      for (NodeValue nv : oldNode.getValues().values()) {
        newNode.addValue(nv);
      }

      // insert new node
      add(newNode);

      // rename all children
      for (Node n : oldNode.getChildren().values()) {
        rename(n.getPath(), newNode.getPath() + GenericController.PATH_DELIMITER + n.getName());
      }

      // remove old node
      delete(oldNode.getPath());
    }
  }

  @Override
  public Node delete(String nodeName) throws StorageException {
    synchronized (nodes) {
      if (nodes.get(nodeName) == null) {
        throw new StorageException("Node does not exist");
      }
      if (!"".equals(nodes.get(nodeName).getChildNodesCsv())) {
        throw new StorageException("Node does have children... cannot remove " + nodeName);
      }
      Node n = nodes.remove(nodeName);
      // add tombstone
      Node tombstone = new NodeImpl(n.getPath(), true);
      tombstone.setVisibility(n.getVisibility());
      nodes.put(n.getPath(), tombstone);
      if (n.getParentPath() != null && !"".equals(n.getParentPath())) {
        nodes.get(n.getParentPath()).removeChild(n.getName());
      }
      return n;
    }
  }

  @Override
  public NodeValue getValue(String path, String key) throws StorageException {
    Node ret = get(path);
    if (ret.isTombstone()) {
      throw new StorageException("Not does not exist");
    }
    return ret.getValues().get(key);
  }

  @Override
  public List<Node> search(SearchCriteria criteria) throws StorageException {
    List<Node> l = new Vector<>();
    for (Map.Entry<String, Node> e : nodes.entrySet()) {
      if (criteria.evaluate(e.getValue())) {
        l.add(e.getValue());
      }
    }
    return l;
  }

  @Override
  public void close() {
    // not required for the dummy wrapper as there is no persistence
  }

  @Override
  public void flush() {
    // not required for the dummy wrapper as there is no persistence
  }

  @Override
  public void zap() {
    synchronized (nodes) {
      nodes.clear();
    }
  }

  /**
   * <p>Saves the current state of the DummyMapper in a file.</p>
   */
  private void saveState() {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      synchronized (nodes) {
        for (Node n : nodes.values()) {
          n.toByteArrayStream(out);
        }
      }
      new File().writeAllBytes("DummyMapper.tmp.db", out.toByteArray());
      // TODO renaming of Files is not available in wrapper
      // File f = new File("DummyMapper.tmp.db");
      // f.renameTo("DummyMapper.db");
    } catch (IOException ioe) {
      System.err.println("Could not store DummyMapper data");
      ioe.printStackTrace();
    } catch (Throwable throwable) {
      System.err.print("Error while writting File");
      throwable.printStackTrace();
    }
  }

  /**
   * <p>Restores the state of the DummyMapper from a previous saved state.</p>
   */
  private void restoreState() {
    // check if either permanent or temporary file available
    // TODO after file class is adapted
    /*
    File f = new File("DummyMapper.db");
    if (!f.exists() || !f.canRead()) {
      // check for temporary file
      f = new File("DummyMapper.tmp.db");
      if(!f.exists() || !f.canRead()) {
        // Either files are corrupt or nothing has been stored
        // -> create the files and finish
        f.createNewFile();
        // TODO how to exit here? throw exeption?
      }
    }*/

    try {
      //byte[] buff = new File().readAllBytes(f.getname());
      byte[] buff = new File().readAllBytes("DummyMapper.db");
      if (buff == null) {
        buff = new byte[0];
      }
      ByteArrayInputStream in = new ByteArrayInputStream(buff);
      Map<String, Node> restoredNodes = new HashMap<>();
      try {
        while (true) {
          Node n = NodeImpl.fromByteArrayStream(in);
          restoredNodes.put(n.getPath(), n);
        }
      } catch (IOException ioe) {
        // last node reached so nothing to do
      }
      // add all restored nodes
      synchronized (nodes) {
        nodes.clear();
        nodes.putAll(restoredNodes);
      }
    } catch (Throwable e) {
      System.err.println("Could not read DummyMapper database file");
      e.printStackTrace();
    }
  }

  /**
   * Creates a daemon thread that stores DummyMapper data at least every 30s.
   */
  private void continuousPersistence() {
    Thread persistence = new Thread(() -> {
      saveState();
      // wait
      try {
        // wait 30s
        Thread.sleep(30000);
      } catch (InterruptedException ie) {
        // nothing to do
      }
    });
    persistence.setDaemon(true);
    persistence.start();
  }
}
