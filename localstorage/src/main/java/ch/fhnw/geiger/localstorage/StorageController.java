package ch.fhnw.geiger.localstorage;

import ch.fhnw.geiger.localstorage.db.data.Node;
import ch.fhnw.geiger.localstorage.db.data.NodeValue;
import java.util.List;

/**
 * <p>Generic implementation of a convenient storage controller providing persistence to a
 * mapper backend.</p>
 */
public interface StorageController {

  /**
   * <p>Fetches a node by its path.</p>
   *
   * @param path the path of the node to be fetched
   * @return The requested node
   */
  Node get(String path) throws StorageException;

  /**
   * <p>Add StorageNode to data.</p>
   *
   * @param node is the node to add
   */
  void add(Node node) throws StorageException;

  /**
   * <p>Update a StorageNode inside the data.</p>
   *
   * @param node is the node to updated
   */
  void update(Node node) throws StorageException;

  /**
   * <p>Remove a StorageNode from the data.</p>
   *
   * @param path the name of the starage node to be removed
   * @return the removed node or null if node doesn't exist
   */
  Node delete(String path) throws StorageException;

  /**
   * <p>Rename a node identified by a path.</p>
   *
   * <p>This call renames a node. The new name may be a name only or a fully qualified path.
   * The later operation noves the node and all child objects.</p>
   *
   * @param oldPath the old path of the node
   * @param newName the new name or new path of the node
   */
  void rename(String oldPath, String newName) throws StorageException;

  /**
   * <p>Get a single value from a node.</p>
   *
   * @param path the path of the node to add the value
   * @param key  the key of the value to be retrieved
   * @return true if value was added, false otherwise
   * @throws StorageException if the node or the object does not exist or
   *                          the storage backend encounters an error
   */
  NodeValue getValue(String path, String key) throws StorageException;

  /**
   * <p>Add NodeValueObject to StorageNode.</p>
   *
   * @param path  the path of the node to add the value
   * @param value the NodeValueObject to add
   */
  void addValue(String path, NodeValue value) throws StorageException;

  /**
   * <p>Updates one NodeValueObject with a new NodeValueObject.</p>
   *
   * <p>It couples all fields except key. The key is used to search the NodeValueObject
   * to update.</p>
   *
   * <p>TODO maybe whole object can be removed and the new one added? (might change object uuid)</p>
   *
   * @param nodeName the node to update
   * @param value    the new NodeValueObject used for updating
   */
  void updateValue(String nodeName, NodeValue value) throws StorageException;

  /**
   * <p>Updates one NodeValueObject with a new NodeValueObject by copying all fields except key.</p>
   *
   * <p>The key is used to search the NodeValueObject to update.</p>
   *
   * <p>TODO maybe whole object can be removed and the new one added? (might change object uuid)</p>
   *
   * @param path the path to the node
   * @param key  the key to be removed from the value
   * @return the removed node value
   */
  NodeValue removeValue(String path, String key) throws StorageException;

  /**
   *<p> Search for nodes that meet the criteria.</p>
   *
   * @param criteria a list of SearchCriteria
   * @return List of StorageNodes, list could be empty
   */
  List<Node> search(SearchCriteria criteria) throws StorageException;

  /**
   * <p>Closes all database connections and flushes the content.</p>
   */
  void close() throws StorageException;

  /**
   * <p>Flushes all values to the backend.</p>
   */
  void flush() throws StorageException;

  /**
   * <p>Clear the entire storage.</p>
   *
   * <p>Handle with care... there is no undo function.</p>
   */
  void zap();
}
