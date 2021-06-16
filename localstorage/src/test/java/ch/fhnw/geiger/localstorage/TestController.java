package ch.fhnw.geiger.localstorage;

import static ch.fhnw.geiger.localstorage.Visibility.RED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import ch.fhnw.geiger.localstorage.db.GenericController;
import ch.fhnw.geiger.localstorage.db.data.Node;
import ch.fhnw.geiger.localstorage.db.data.NodeImpl;
import ch.fhnw.geiger.localstorage.db.data.NodeValue;
import ch.fhnw.geiger.localstorage.db.data.NodeValueImpl;
import ch.fhnw.geiger.localstorage.db.mapper.DummyMapper;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

/***
 * <p>Test the controller capabilities.</p>
 */
public class TestController {

  // TODO add test reflecting the recursion for child nodes

  private final static Object semaphore = new Object();
  public GenericController controller = null;

  @Before
  public void setupTest() throws StorageException {
    // clear all stored objects
    synchronized (semaphore) {
      if (controller == null) {
        controller = new GenericController("testOwner", new DummyMapper());
      }
      controller.zap();
    }
  }
  @Test
  public void testOwnerUpdateOnNode() throws StorageException {
    System.out.println("## Testing controller in " + (new Object() {
    }).getClass().getEnclosingMethod().getName());
    Node node = new NodeImpl("testNode1", "");
    controller.add(node);
    node = controller.get(node.getPath());
    assertEquals("testing that owner is set upon adding", "testOwner", node.getOwner());
  }

  @Test
  public void testStorageNodeCreate() throws StorageException {
    controller.add(new NodeImpl("testNode1", ""));

    // fetch stored node
    Node storedNode = controller.get(":testNode1");

    // check results
    assertEquals("testOwner", storedNode.getOwner());
    assertEquals("testNode1", storedNode.getName());
    assertEquals(":testNode1", storedNode.getPath());
    assertSame(RED, storedNode.getVisibility());
  }

  @Test
  public void testStorageNodeAdd() throws StorageException {
    controller.add(new NodeImpl("parent1", ""));
    controller.add(new NodeImpl("name2", ":parent1"));

    // get the record
    Node storedNode = controller.get(":parent1:name2");

    // check results
    assertEquals("testOwner", storedNode.getOwner());
    assertEquals("name2", storedNode.getName());
    assertEquals(":parent1:name2", storedNode.getPath());
    assertSame(RED, storedNode.getVisibility());
  }

  /**
   * depends on correct functionality of the StorageController.create() function
   */
  @Test
  public void testStorageNodeUpdate() throws StorageException {
    // create original node
    controller.add(new NodeImpl("parent1", ""));

    // updated Node with different visibility children
    NodeImpl node = new NodeImpl("testNode1", ":parent1", Visibility.GREEN);
    controller.add(node);
    assertEquals("testOwner", node.getOwner());
    NodeImpl sn = new NodeImpl("testChild1", ":parent1:testNode1");
    controller.add(sn);
    node.setVisibility(Visibility.RED);
    node.addChild(sn);

    // update with node from above
    controller.update(node);

    // get the record
    Node storedNode = controller.get(":parent1:testNode1");

    // check results
    assertEquals("testOwner", storedNode.getOwner());
    assertEquals("testNode1", storedNode.getName());
    assertEquals(":parent1:testNode1", storedNode.getPath());
    assertEquals("testChild1", storedNode.getChildNodesCsv());
    assertSame(RED, storedNode.getVisibility());
  }

  @Test
  public void testValueUpdate() throws StorageException {
    try {
      controller.add(new NodeImpl("testNode1", ":parent1"));
      fail("Should raise an exception as parent node does not exist");
    } catch (StorageException e) {
      // this is the expected behavior as the parent node does not exist
    }
    NodeImpl[] n = new NodeImpl[]{
        new NodeImpl("parent1"),
        new NodeImpl("testNode1", ":parent1")};
    for (NodeImpl tn : n) {
      System.out.println("## creating new node " + tn.getPath() + " (parent of "
          + tn.getParentPath() + ")");
      controller.add(tn);
    }

    // add a value
    System.out.println("## adding value");
    controller.addValue(":parent1:testNode1", new NodeValueImpl("key1", "valueFirst"));

    // update value
    System.out.println("## updating value");
    NodeValue value2 = new NodeValueImpl("key1", "valueSecond");
    controller.updateValue(":parent1:testNode1", value2);

    // get the record
    System.out.println("## testing updated value");
    Node n2 = controller.get(":parent1:testNode1");
    assertEquals("stored values do not match", value2.getValue(),
            n2.getValue(value2.getKey()).getValue());

    try {
      System.out.println("## testing removal of node with child nodes ("
          + n[n.length - 1].getPath() + ")");
      controller.delete(n[0].getPath());
      fail("should raise an exception as child classes exist");
    } catch (StorageException e) {
      // this is expected as the node has sub-nodes
    }

    Collections.reverse(Arrays.asList(n));
    for (NodeImpl tn : n) {
      System.out.println("## removing node " + tn.getPath() + " (parent of "
          + tn.getParentPath() + ")");
      controller.delete(tn.getPath());
    }
  }

  @Test
  public void testStorageNodeRemove() throws StorageException {
    controller.add(new NodeImpl("parent1", ""));
    NodeImpl node = new NodeImpl("name1", ":parent1");
    NodeValue nv = new NodeValueImpl("key", "value");
    node.addValue(nv);
    controller.add(node);
    Node removed = controller.delete(":parent1:name1");

    // check nodes
    assertEquals(node, removed);
    assertThrows(StorageException.class, () -> controller.get(removed.getPath()));

    // check values
    assertEquals(nv, removed.getValue("key"));
    assertThrows(NullPointerException.class, () -> controller.getValue(removed.getPath(), "key"));
  }

  @Test
  public void testStorageNodeRemoveWithChild() throws StorageException {
    controller.add(new NodeImpl("parent1", ""));
    NodeImpl node = new NodeImpl("name1", ":parent1");
    // add child
    NodeImpl childNode = new NodeImpl("child1", ":parent1:name1");
    node.addChild(childNode);
    controller.add(node);
    controller.add(childNode);

    assertThrows(StorageException.class, () -> controller.delete(":parent1:name1"));

    // check if node still exists
    assertEquals(node, controller.get(":parent1:name1"));
  }

  @Test
  public void testStorageNodeSearch() {

  }

  @Test
  public void testRegisterChangeListener() {

  }

  @Test
  public void testNotifyChangeListener() {

  }

  @Test
  public void testRenameNode() throws StorageException {
    NodeImpl[] nodes = new NodeImpl[]{
        new NodeImpl("renameTests"),
        new NodeImpl("name1", ":renameTests"),
        new NodeImpl("name11", ":renameTests:name1"),
        new NodeImpl("name2", ":renameTests"),
        new NodeImpl("name21", ":renameTests:name2"),
        new NodeImpl("name3", ":renameTests")};
    for (NodeImpl n : nodes) {
      controller.add(n);
    }
    // rename by name
    controller.rename(":renameTests:name1", "name1a");
    // rename by path
    controller.rename(":renameTests:name2", ":renameTests:name2a");

    // check old nodes
    assertThrows(StorageException.class, () -> controller.get(":renameTests:name1"));
    assertThrows(StorageException.class, () -> controller.get(":renameTests:name2"));

    // check new nodes
    assertNotNull("renaming node seems unsuccessful (new node missing)",
            controller.get(":renameTests:name1a"));
    assertNotNull("renaming node seems unsuccessful (new node missing)",
        controller.get(":renameTests:name2a"));

    // check name
    assertEquals("renaming node seems unsuccessful (new node name wrong)",
            "name1a", controller.get(":renameTests:name1a").getName());
    assertEquals("renaming node seems unsuccessful (new node name wrong)",
        "name2a", controller.get(":renameTests:name2a").getName());

    // check path
    assertEquals("renaming node seems unsuccessful (new node path wrong)",
            ":renameTests:name1a", controller.get(":renameTests:name1a").getPath());
    assertEquals("renaming node seems unsuccessful (new node path wrong)",
        ":renameTests:name2a", controller.get(":renameTests:name2a").getPath());

    // check child nodes
    assertNotNull("renaming node seems unsuccessful (sub-node missing)",
            controller.get(":renameTests:name1a:name11"));
    assertNotNull("renaming node seems unsuccessful (sub-node missing)",
        controller.get(":renameTests:name2a:name21"));

    // check child node name
    assertEquals("renaming node seems unsuccessful (sub-node name wrong)",
            "name11", controller.get(":renameTests:name1a:name11").getName());
    assertEquals("renaming node seems unsuccessful (sub-node name wrong)",
        "name21", controller.get(":renameTests:name2a:name21").getName());

    // check child node path
    assertEquals("renaming node seems unsuccessful (sub-node path wrong)",
            ":renameTests:name1a:name11", controller.get(":renameTests:name1a:name11").getPath());
    assertEquals("renaming node seems unsuccessful (sub-node path wrong)",
        ":renameTests:name2a:name21", controller.get(":renameTests:name2a:name21").getPath());

    // test rename of non existing nodes
    assertThrows(StorageException.class,
            () -> controller.rename(":renameTests:name4", ":renameTests:name4a"));
    assertThrows(StorageException.class,
            () -> controller.rename(":renameTests:name4", "name4a"));

    // test rename to an existing node
    assertThrows(StorageException.class,
            () -> controller.rename(":renameTests:name2a", ":renameTests:name3"));
    assertThrows(StorageException.class,
            () -> controller.rename(":renameTests:name2a", "name3"));
  }

  @Test
  public void testRenameNodeWithValues() throws StorageException {
    NodeImpl[] nodes = new NodeImpl[]{
        new NodeImpl("renameTests"),
        new NodeImpl("name1", ":renameTests"),
        new NodeImpl("name2", ":renameTests"),
        new NodeImpl("name21", ":renameTests:name2"),
        new NodeImpl("name3", ":renameTests")
      };

    NodeValue nv = new NodeValueImpl("key", "value");
    NodeValue nv1 = new NodeValueImpl("key1", "value1");
    NodeValue nv2 = new NodeValueImpl("key2", "value2");
    NodeValue nv21 = new NodeValueImpl("key21", "value21");

    nodes[0].addValue(nv);
    nodes[1].addValue(nv1);
    nodes[2].addValue(nv2);
    nodes[3].addValue(nv21);

    for (NodeImpl n : nodes) {
      controller.add(n);
    }
    controller.rename(":renameTests:name2", ":renameTests:name2a");

    // check old node
    assertThrows(StorageException.class, () -> controller.get(":renameTests:name2"));

    assertNotNull("renaming node seems unsuccessful (new node missing)",
            controller.get(":renameTests:name2a"));
    assertEquals("renaming node seems unsuccessful (new node name wrong)",
            "name2a", controller.get(":renameTests:name2a").getName());
    assertEquals("renaming node seems unsuccessful (new node path wrong)",
            ":renameTests:name2a", controller.get(":renameTests:name2a").getPath());
    assertNotNull("renaming node seems unsuccessful (sub-node missing)",
            controller.get(":renameTests:name2a:name21"));
    assertEquals("renaming node seems unsuccessful (sub-node name wrong)",
            "name21", controller.get(":renameTests:name2a:name21").getName());
    assertEquals("renaming node seems unsuccessful (sub-node path wrong)",
            ":renameTests:name2a:name21", controller.get(":renameTests:name2a:name21").getPath());

    // check values
    assertEquals("value lost on parent", nv,
            controller.get(":renameTests").getValue("key"));
    assertEquals("value lost on sibling", nv1,
            controller.get(":renameTests:name1").getValue("key1"));
    assertEquals("value lost moved node", nv2,
            controller.get(":renameTests:name2a").getValue("key2"));
    assertEquals("value lost on sub-node", nv21,
            controller.get(":renameTests:name2a:name21").getValue("key21"));

    // check old values
    assertThrows(NullPointerException.class,
            () -> controller.getValue(":renameTests:name2", "key2"));
    assertThrows(NullPointerException.class,
            () -> controller.getValue(":renameTests:name2:name21", "key21"));
  }

}
