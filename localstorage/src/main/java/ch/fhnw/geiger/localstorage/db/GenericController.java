package ch.fhnw.geiger.localstorage.db;

import ch.fhnw.geiger.localstorage.ChangeRegistrar;
import ch.fhnw.geiger.localstorage.EventType;
import ch.fhnw.geiger.localstorage.SearchCriteria;
import ch.fhnw.geiger.localstorage.StorageController;
import ch.fhnw.geiger.localstorage.StorageException;
import ch.fhnw.geiger.localstorage.StorageListener;
import ch.fhnw.geiger.localstorage.db.data.Node;
import ch.fhnw.geiger.localstorage.db.data.NodeImpl;
import ch.fhnw.geiger.localstorage.db.data.NodeValue;
import ch.fhnw.geiger.localstorage.db.data.NodeValueImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

/**
 * <p>This Class Acts as an intermediate class to relay storageRequests to the
 * StorageMapper.</p>
 *
 * <p>This class contains the interaction API every actor (sensor, shields, UI,...)
 * calls methods from this controller to manipulate data.</p>
 *
 * @author Sacha
 * @version 0.1
 */
public class GenericController implements StorageController, ChangeRegistrar {

  /**
   * the path delimiter.
   */
  public static final String PATH_DELIMITER = ":";

  /**
   * the default owner of all newly created nodes.
   */
  private final String owner;

  /**
   * a map of all registered listeners for changes.
   */
  private final Map<SearchCriteria, StorageListener> listeners = new HashMap<>();

  /**
   * the storage mapper to be used.
   */
  private final StorageMapper mapper;

  /**
   * <p>Construct the controller according to the database that is used.</p>
   *
   * @param owner  the owner to be used for all requests
   * @param mapper the database mapper to use
   */
  public GenericController(String owner, StorageMapper mapper) {
    this.owner = owner;
    this.mapper = mapper;
    this.mapper.setController(this);
    initMapper();
  }

  private void initMapper() throws StorageException {
    final String[] baseNodes = new String[]{
        "", ":Devices", ":Users", ":Enterprise", ":Keys", ":Global", ":Local"
      };
    for (String nodeName : baseNodes) {
      try {
        Node n = mapper.get(nodeName);
      } catch (StorageException e) {
        // node does not exist and therefore we create it
        mapper.add(new NodeImpl(nodeName));
      }
    }

    // check if current user exists
    Node localNode = mapper.get(":Local");
    NodeValue uuid = localNode.getValue("currentUser");
    if (uuid == null) {
      // create new default user
      uuid = new NodeValueImpl("currentUser", UUID.randomUUID().toString());
      localNode.addValue(uuid);
      mapper.update(localNode);
    }

    // check if current user node exists
    String userNodeName = ":Users:" + uuid.getValue();
    try {
      mapper.get(userNodeName);
    } catch (StorageException se) {
      mapper.add(new NodeImpl(userNodeName));
    }

    // check if current device exists
    localNode = mapper.get(":Local");
    uuid = localNode.getValue("currentDevice");
    if (uuid == null) {
      // create new default device
      uuid = new NodeValueImpl("currentDevice", UUID.randomUUID().toString());
      localNode.addValue(uuid);
      mapper.update(localNode);
    }

    // check if current device node exists
    String deviceNodeName = ":Devices:" + uuid.getValue();
    try {
      mapper.get(deviceNodeName);
    } catch (StorageException se) {
      mapper.add(new NodeImpl(deviceNodeName));
    }

  }

  @Override
  public Node get(String path) throws StorageException {
    return mapper.get(path);
  }

  @Override
  public void add(Node node) throws StorageException {
    // make sure that there is an owner set
    if (node.getOwner() == null || "".equals(node.getOwner())) {
      node.setOwner(owner);
    }

    // add object
    mapper.add(node);

    checkListeners(EventType.CREATE, null, node, null, null);
  }

  @Override
  public void update(Node node) throws StorageException {
    // make sure that there is an owner set
    if (node.getOwner() == null || "".equals(node.getOwner())) {
      node.setOwner(owner);
    }

    // get old node for update events
    Node oldNode = mapper.get(node.getPath());

    // write node
    mapper.update(node);

    checkListeners(EventType.UPDATE, oldNode, node, null, null);

    // any child that is not a skeleton will be handled as new or changed
    for (Node child : node.getChildren().values()) {
      if (!child.isSkeleton()) {
        try {
          add(child);
        } catch (StorageException e) {
          // node already exists, therefore it was changed
          update(child);
        }
      }
    }
  }

  @Override
  public Node delete(String path) throws StorageException {
    Node ret = mapper.remove(path);
    checkListeners(EventType.DELETE, ret, null, null, null);
    return ret;
  }

  @Override
  public void rename(String oldPath, String newPath) throws StorageException {
    NodeImpl oldNode = (NodeImpl) mapper.get(oldPath);
    mapper.rename(oldPath, newPath);
    NodeImpl newNode = (NodeImpl) mapper.get(newPath);
    checkListeners(EventType.RENAME, oldNode, newNode, null, null);
  }

  @Override
  public NodeValue getValue(String path, String key) throws StorageException {
    return mapper.getValue(path, key);
  }

  @Override
  public void addValue(String nodeName, NodeValue newValue) throws StorageException {
    Node oldNode = mapper.get(nodeName);
    NodeValue oldValue = oldNode.getValue(newValue.getKey());
    if (oldValue != null) {
      throw new StorageException("value \"" + newValue.getKey() + "\" is already set");
    }
    Node newNode = oldNode.deepClone();
    newNode.addValue(newValue);
    mapper.update(newNode);
    checkListeners(EventType.UPDATE, oldNode, newNode, null, newValue);
  }

  @Override
  public void updateValue(String nodeName, NodeValue newValue) throws StorageException {
    Node oldNode = mapper.get(nodeName);
    NodeValue oldValue = oldNode.getValue(newValue.getKey());
    if (oldValue == null) {
      throw new StorageException("value \"" + newValue.getKey() + "\" does not yet exist");
    }
    Node newNode = oldNode.deepClone();
    newNode.removeValue(newValue.getKey());
    newNode.addValue(newValue);
    mapper.update(newNode);
    checkListeners(EventType.UPDATE, oldNode, newNode, oldValue, newValue);
  }

  @Override
  public NodeValue removeValue(String nodeName, String key) throws StorageException {
    Node oldNode = mapper.get(nodeName);
    NodeValue oldValue = oldNode.getValue(key);
    if (oldValue == null) {
      throw new StorageException("value \"" + key + "\" does not yet exist");
    }
    Node newNode = oldNode.deepClone();
    newNode.removeValue(key);
    mapper.update(newNode);
    checkListeners(EventType.UPDATE, oldNode, newNode, oldValue, null);
    return oldValue;
  }

  @Override
  public List<Node> search(SearchCriteria criteria) throws StorageException {
    return mapper.search(criteria);
  }

  @Override
  public void close() {
    mapper.close();
  }

  public void flush() {
    // nothing to do with H2 (flushes roughly after a second
  }

  private void checkListeners(final EventType event, final Node oldNode, final Node newNode,
                              NodeValue oldValue, NodeValue newValue) {
    if (oldNode == null || newNode == null || !oldNode.equals(newNode)) {
      synchronized (listeners) {
        for (Map.Entry<SearchCriteria, StorageListener> e : listeners.entrySet()) {
          if (
              (
                  oldNode != null && newNode != null
                      && (e.getKey().evaluate(oldNode) || e.getKey().evaluate(newNode))
              )
                  || (oldNode != null && e.getKey().evaluate(oldNode))
                  || (newNode != null && e.getKey().evaluate(newNode))
          ) {
            new Thread(() -> e.getValue().gotStorageChange(event, oldNode, newNode)).start();
          }
        }
      }
    }
  }

  @Override
  public void registerChangeListener(StorageListener listener, SearchCriteria criteria) {
    if (listener == null) {
      throw new NullPointerException("listener may not be null");
    }
    if (criteria == null) {
      throw new NullPointerException("criteria may not be null");
    }
    synchronized (listeners) {
      listeners.put(criteria, listener);
    }
  }

  @Override
  public SearchCriteria[] deregisterChangeListener(StorageListener listener) {
    if (listener == null) {
      throw new NullPointerException("listener may not be null");
    }
    List<SearchCriteria> remove = new Vector<>();
    synchronized (listeners) {
      for (Map.Entry<SearchCriteria, StorageListener> e : listeners.entrySet()) {
        if (e.getValue() == listener) {
          remove.add(e.getKey());
        }
      }
      for (SearchCriteria c : remove) {
        listeners.remove(c);
      }
      return remove.toArray(new SearchCriteria[remove.size()]);
    }
  }

  public void zap() {
    mapper.zap();
    initMapper();
  }

}
