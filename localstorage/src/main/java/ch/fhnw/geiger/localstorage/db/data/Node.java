package ch.fhnw.geiger.localstorage.db.data;

import ch.fhnw.geiger.localstorage.StorageController;
import ch.fhnw.geiger.localstorage.StorageException;
import ch.fhnw.geiger.localstorage.Visibility;
import java.util.Map;

/**
 * <p>Interface representing a single node in the storage.</p>
 */
public interface Node {

  /**
   * <p>Gets the name of the current node.</p>
   *
   * @return the name of the current node without the path prefix
   */
  String getName();

  /**
   * <p>Gets the parent path of the node.</p>
   *
   * @return the parent path of the current node
   */
  String getParentPath();

  /**
   * <p>Gets the full path with name of the current node.</p>
   *
   * @return the fully qualified name of the current node
   */
  String getPath();

  /**
   * <p>Gets the owner of the current object.</p>
   *
   * @return string representation of the owner
   */
  String getOwner();

  /**
   * <p>Sets the owner string.</p>
   *
   * @param newOwner the string representation of the previously set owner
   * @return the previously set owner
   */
  String setOwner(String newOwner);

  /**
   * <p>Gets the current visibility according to the TLP protocol.</p>
   *
   * @return the current visibility
   */
  Visibility getVisibility();

  /**
   * <p>Sets the visibility of the node.</p>
   *
   * @param newVisibility the new visibility
   * @return the visibility set previously
   */
  Visibility setVisibility(Visibility newVisibility);

  /**
   * <p>Add a key/value pair to the node.</p>
   *
   * <p>Adds a K/V tuple to the node. The key must not
   * exist prior adding.</p>
   *
   * @param value a NodeValue object representing the K/V pair
   * @throws StorageException if key already exists
   */
  void addValue(NodeValue value) throws StorageException;

  /**
   * <p>Get a specific value of the node.</p>
   *
   * @param key the key to be looked up
   * @return the requested value or null if not found
   */
  NodeValue getValue(String key);

  /**
   * <p>Update a specific value of the node.</p>
   *
   * @param value the key to be updated
   * @return the requested value or null if not found
   */
  NodeValue updateValue(NodeValue value);

  /**
   * <p>Removes a value from the node.</p>
   *
   * @param key the key of the value to be removed
   * @return the removed node value or null if not found
   */
  NodeValue removeValue(String key);

  /**
   * <p>Get a deep copy of all values stored in the node.</p>
   *
   * @return a map of all values
   */
  Map<String, NodeValue> getValues();

  /***
   * <p>Adds a child node to this node.</p>
   *
   * @param node the child node to be added
   */
  void addChild(Node node);

  /***
   * <p>Gets a child node from the current node.</p>
   *
   * @param name the name of the child node to fetch
   * @return the requested child node or null if the node does not exist
   */
  Node getChild(String name);


  /***
   * <p>Removes a child node from this node.</p>
   *
   * @param name the name of the child node to be removed
   */
  void removeChild(String name);

  /**
   * <p>Get a map of all existing child nodes.</p>
   *
   * @return the map containing all child nodes
   */
  Map<String, Node> getChildren();

  String getChildNodesCsv();

  /**
   * <p>Returns true if the current node is not yet materialized.</p>
   *
   * @return true if the current node is a skeleton only
   */
  boolean isSkeleton();

  /**
   * <p>get the controller needed for materializing the node if required.</p>
   *
   * @return the controller
   */
  StorageController getController();

  /**
   * <p>Sets the controller needed for materializing the node if required.</p>
   *
   * @param controller the controller to be set
   * @return the previously set controller
   */
  StorageController setController(StorageController controller);

  /**
   * <p>Update all data of the node with the data of the given node.</p>
   *
   * @param n2 the node whose values should be copied
   */
  void update(Node n2);

  /**
   * <p>Create a deeep clone of the current node.</p>
   *
   * @return the cloned node
   */
  NodeImpl deepClone();

}
